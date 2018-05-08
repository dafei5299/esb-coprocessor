#!/bin/bash

export LANG=zh_CN.utf8
script_src=`readlink -f $0`
base_dir=`dirname $script_src`
bak_dir="$base_dir/backup"
mkdir -p $bak_dir
xtra_opts="--use-memory=1G --rsync"
primary_addr="192.168.151.158"
primary_script="/mysqldata/backup/primary.sh"
secondary_addr="192.168.111.62"
secondary_script="/data/mariadb/backup/secondary.sh"

create() {
   client_addr=`ifconfig eth0 | grep -oE 'inet addr:[0-9.]+' | awk -F: '{print $2}'`
   client_port="9999"
   tmp_file="$base_dir/backup.tar.gz"
   nc_pid="$base_dir/nc.pid"
   output="$base_dir/output"

   # receive backup package
   /usr/bin/nc -dl $client_port 1>$tmp_file & echo $! >$nc_pid
   ssh $primary_addr "$primary_script create $client_addr $client_port" &>$output
   if [ $? -ne 0 ]; then
      kill $(cat $nc_pid) 2>/dev/null
      cat $output
      rm -f $tmp_file $nc_pid $output
      exit 1
   fi
   while (kill -0 $(cat $nc_pid) 2>/dev/null); do
      sleep 1
   done
   rm -f $nc_pid

   # check md5
   timestamp=`awk '/^timestamp:/{print $2}' $output`
   md5_1=`awk '/^md5:/{print $2}' $output`
   md5_2=`md5sum $tmp_file | awk '{print $1}'`
   if [ $md5_1 != $md5_2 ]; then
      echo "ERR"
      echo "md5 not match, transfer maybe corrupted, server: $md5_1, client: $md5_2"
      ssh $primary_addr "$primary_script delete $timestamp"
      rm -f $tmp_file $output
      exit 1
   fi

   mv $tmp_file "$bak_dir/$timestamp.tar.gz"
   cat $output
   rm -f $output
}

delete() {
   timestamp=$1
   output="$base_dir/output"

   if [ -f "$bak_dir/$timestamp.tar.gz" ]; then
      rm -f "$bak_dir/$timestamp.tar.gz"
      ssh $primary_addr "$primary_script delete $timestamp" &>$output
      if [ $? -ne 0 ]; then
         echo "ERR"
         echo "delete remote backup failure, timestamp: $timestamp"
         cat $output
         rm -f $output
         exit 1
      fi
      rm -f $output
   fi
   echo "OK"
}

checkpoint() {
   base="$1"
   incrs="$2"
   tmp_dir="$base_dir/tmp"
   mkdir -p $tmp_dir
   output="$base_dir/output"
   md5="$base_dir/md5"

   # check backups exist or not
   for ts in `echo "$base,$incrs" | tr ',' ' '`; do
      if [ ! -f "$bak_dir/$ts.tar.gz" ]; then
         echo "ERR"
         echo "backup not found, timestamp: $ts"
         exit 1
      fi
   done

   # prepare base backup
   mkdir "$tmp_dir/$base" && tar zxf "$bak_dir/$base.tar.gz" -C "$tmp_dir/$base" 2>/dev/null
   if ! `grep -qw 'from_lsn = 0' "$tmp_dir/$base/xtrabackup_checkpoints"`; then
      echo "ERR"
      echo "need full-backuped as base point, timestamp: $base"
      rm -rf $tmp_dir
      exit 1
   fi
   if ! `grep -qw 'backup_type = full-prepared' "$tmp_dir/$base/xtrabackup_checkpoints"`; then
      innobackupex $xtra_opts --apply-log --redo-only "$tmp_dir/$base" 2>$output
      if ! `tail -1 $output | grep -q 'completed OK!'`; then
         echo "ERR"
         echo "error occur when prepare base point"
         cat $output
         rm -rf $output $tmp_dir
         exit 1
      fi
      rm -f $output
   fi
   lsn=`awk '/^to_lsn/{print $3}' "$tmp_dir/$base/xtrabackup_checkpoints"`

   # apply increments onto base backup
   for ts in `echo $incrs | tr ',' ' '`; do
      mkdir "$tmp_dir/$ts" && tar zxf "$bak_dir/$ts.tar.gz" -C "$tmp_dir/$ts" 2>/dev/null
      incr_lsn=`awk '/^from_lsn/{print $3}' "$tmp_dir/$ts/xtrabackup_checkpoints"`
      if [ $lsn -ne $incr_lsn ]; then
         echo "ERR"
         echo "lsn need to be sequential, timestamp: $ts, expected: $lsn, actual: $incr_lsn"
         rm -rf $tmp_dir
         exit 1
      fi
      innobackupex $xtra_opts --apply-log --redo-only "$tmp_dir/$base" --incremental-dir="$tmp_dir/$ts" 2>$output
      if ! `tail -1 $output | grep -q 'completed OK!'`; then
         echo "ERR"
         echo "error occur when apply incremental, timestamp: $ts"
         cat $output
         rm -rf $output $tmp_dir
         exit 1
      fi
      rm -f $output
      lsn=`awk '/^to_lsn/{print $3}' "$tmp_dir/$base/xtrabackup_checkpoints"`
   done

   tar zc -C "$tmp_dir/$base" . | tee >(md5sum >$md5) >"$bak_dir/$ts.tar.gz"
   echo "OK"
   echo "timestamp: $ts"
   echo "size: $(du -sh "$tmp_dir/$base" | awk '{print $1}')"
   echo "md5: $(awk '{print $1}' $md5)"
   checkpoints="$tmp_dir/$base/xtrabackup_checkpoints"
   echo "type: $(awk '/^backup_type/{print $3}' $checkpoints)"
   echo "from_lsn: $(awk '/^from_lsn/{print $3}' $checkpoints)"
   echo "to_lsn: $(awk '/^to_lsn/{print $3}' $checkpoints)"
   binlog_info=`[ -f "$tmp_dir/$base/xtrabackup_binlog_info" ] && echo yes || echo no`
   echo "binlog_info: $binlog_info"
   rm -rf $tmp_dir $md5
}

restore() {
   client_addr=`ifconfig eth0 | grep -oE 'inet addr:[0-9.]+' | awk -F: '{print $2}'`
   client_port="9999"
   timestamp="$2"
   bak_file="$bak_dir/$timestamp.tar.gz"
   replay_timestamp="$3"
   nc_pid="$base_dir/nc.pid"
   output="$base_dir/output"
   server_addr=$primary_addr
   server_script=$primary_script
   if [ "$1" == "secondary" ]; then
      server_addr=$secondary_addr
      server_script=$secondary_script
   fi

   # check backups exist or not
   if [ ! -f $bak_file ]; then
      echo "ERR"
      echo "backup not found, timestamp: $timestamp"
      exit 1
   fi

   # transfer backup to server
   cat $bak_file | /usr/bin/nc -l $client_port & echo $! >$nc_pid
   md5=`md5sum $bak_file | awk '{print $1}'`
   ssh $server_addr "$server_script restore $client_addr $client_port $md5 $replay_timestamp" &>$output
   if [ $? -ne 0 ]; then
      kill $(cat $nc_pid) 2>/dev/null
      cat $output
      rm -f $nc_pid $output
      exit 1
   fi
   while (kill -0 $(cat $nc_pid) 2>/dev/null); do
      sleep 1
   done
   rm -f $nc_pid
   cat $output
   rm -f $output
}

sync() {
   timestamp="$1"
   output2="$base_dir/output2"
   output3="$base_dir/output3"

   # restore secondary
   $script_src restore secondary $timestamp &>$output2
   if [ $? -ne 0 ]; then
      cat $output2
      rm -f $output2
      exit 1
   fi

   # start primary
   ssh $primary_addr "$primary_script replicate" &>$output3
   if [ $? -ne 0 ]; then
      cat $output3
      rm -f $output3
      exit 1
   fi

   cat $output2
   rm -f $output2 $output3
}

usage() {
   echo "Usage:"
   echo "  ${0##*/} create"
   echo "  ${0##*/} delete <timestamp>"
   echo "  ${0##*/} checkpoint <base_timestamp> <incr_timestamps>"
   echo "  ${0##*/} restore <primary|secondary> <timestamp> [<replay_timestamp>]"
   echo "  ${0##*/} sync <timestamp>"
   exit 1
}

[ $# -ge 1 ] || usage
case $1 in
   create)
      [ $# -ge 1 ] || usage
      create
      ;;
   delete)
      [ $# -ge 2 ] || usage
      delete $2
      ;;
   checkpoint)
      [ $# -ge 3 ] || usage
      checkpoint $2 $3
      ;;
   restore)
      [ $# -ge 3 ] || usage
      restore $2 $3 $4
      ;;
   sync)
      [ $# -ge 2 ] || usage
      sync $2
      ;;
   *)
      usage
      ;;
esac
exit 0
