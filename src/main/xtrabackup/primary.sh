#!/bin/bash

export LANG=zh_CN.utf8
script_src=`readlink -f $0`
base_dir=`dirname $script_src`
bak_dir="$base_dir/backup"
mkdir -p $bak_dir
xtra_opts="--defaults-file=/usr/local/etc/my.cnf --use-memory=1G --rsync"
mysql_script="/etc/init.d/mariadb"
mysql_data="/mysqldata/datadir/mariadb"
mysql_opts="--defaults-file=/usr/local/etc/my.cnf"
secondary_host="192.168.111.62"
secondary_port="3307"
secondary_user="repl"
secondary_passwd="repl"

create() {
   client="$1 $2"
   output="$base_dir/output"
   md5="$base_dir/md5"

   # create backup
   [ $(ls $bak_dir | wc -l) -gt 0 ] && xtra_opts="$xtra_opts --incremental"
   sudo innobackupex $xtra_opts $bak_dir 2>$output
   if ! `tail -1 $output | grep -q 'completed OK!'`; then
      echo "ERR"
      cat $output
      rm -f $output
      exit 1
   fi
   rm -f $output

   # transfer to client
   timestamp=`ls $bak_dir | sort | tail -1`
   owner="$(id -un):$(id -gn)"
   sudo chown -R $owner "$bak_dir/$timestamp"
   tar zc -C "$bak_dir/$timestamp" . | tee >(md5sum >$md5) | nc $client
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "transfer to client $client failed"
      rm -rf "$bak_dir/$timestamp" $md5
      exit 1
   fi
   md5_value=`awk '{print $1}' $md5`
   rm -f $md5

   # only retain 3 latest backup
   count=`ls $bak_dir | wc -l`
   if [ $count -gt 3 ]; then
      let count-=3
      (cd $bak_dir; ls | sort | head -$count | xargs rm -rf)
   fi

   echo "OK"
   echo "timestamp: $timestamp"
   echo "size: $(du -sh "$bak_dir/$timestamp" | awk '{print $1}')"
   echo "md5: $md5_value"
   checkpoints="$bak_dir/$timestamp/xtrabackup_checkpoints"
   echo "type: $(awk '/^backup_type/{print $3}' $checkpoints)"
   echo "from_lsn: $(awk '/^from_lsn/{print $3}' $checkpoints)"
   echo "to_lsn: $(awk '/^to_lsn/{print $3}' $checkpoints)"
   binlog_info=`[ -f "$bak_dir/$timestamp/xtrabackup_binlog_info" ] && echo yes || echo no`
   echo "binlog_info: $binlog_info"
}

delete() {
   timestamp=$1

   if [ -d "$bak_dir/$timestamp" ]; then
      rm -rf "$bak_dir/$timestamp"
   fi
}

restore() {
   client="$1 $2"
   md5_1="$3"
   timestamp="$4"
   md5="$base_dir/md5"
   output="$base_dir/output"
   tmp_file="$base_dir/backup.tar.gz"
   tmp_dir="$base_dir/tmp"

   # receive backup package
   nc -d $client | tee >(md5sum >$md5) >$tmp_file
   md5_2=`awk '{print $1}' $md5`
   if [ $md5_1 != $md5_2 ]; then
      echo "ERR"
      echo "md5 not match, transfer maybe corrupted, client: $md5_1, server: $md5_2"
      rm -f $md5 $tmp_file
   fi
   rm -f $md5

   # prepare backup
   mkdir $tmp_dir && tar zxf $tmp_file -C $tmp_dir 2>/dev/null && rm -f $tmp_file
   if ! `grep -qw 'from_lsn = 0' "$tmp_dir/xtrabackup_checkpoints"`; then
      echo "ERR"
      echo "need full-backuped data"
      rm -rf $tmp_dir
      exit 1
   fi
   innobackupex $xtra_opts --apply-log $tmp_dir 2>$output
   if ! `tail -1 $output | grep -q 'completed OK!'`; then
      echo "ERR"
      cat $output
      rm -rf $output $tmp_dir
      exit 1
   fi

   # replace mysql data, then restart
   start_time=`date +%s`
   sudo $mysql_script stop &>$output
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "shutdown mysql failure"
      cat $output
      rm -rf $output $tmp_dir
      exit 1
   fi
   mysql_data_bak=${mysql_data}_$(date +%Y%m%d%H%M%S)
   sudo mv $mysql_data $mysql_data_bak && sudo mkdir $mysql_data
   sudo innobackupex $xtra_opts --move-back $tmp_dir 2>$output
   if ! `tail -1 $output | grep -q 'completed OK!'`; then
      echo "ERR"
      cat $output
      rm -rf $output $tmp_dir
      exit 1
   fi
   sudo chown -R mysql:mysql $mysql_data
   sudo $mysql_script start &>$output
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "startup mysql failure"
      cat $output
      rm -rf $output $tmp_dir
      exit 1
   fi
   let downtime=$(date +%s)-$start_time

   # replay if necessary
   if [ ! -f "$tmp_dir/xtrabackup_binlog_info" ]; then
      timestamp=""
   fi
   if [ -n "$timestamp" ]; then
      binlogfile=`awk '{print $1}' "$tmp_dir/xtrabackup_binlog_info"`
      binlogpos=`awk '{print $2}' "$tmp_dir/xtrabackup_binlog_info"`
      $script_src replay $mysql_data_bak $binlogfile $binlogpos $timestamp &>$output
      if [ $? -ne 0 ]; then
         cat $output
         rm -f $output
         (cd $bak_dir; ls | xargs rm -rf)
         mv $tmp_dir "$bak_dir/$(date -d '1 second ago' +%Y-%m-%d_%H-%M-%S)"
         exit 1
      fi
   fi

   echo "OK"
   echo "downtime: $downtime"
   echo "replay: $([ -n "$timestamp" ] && echo yes || echo no)"
   rm -f $output
   (cd $bak_dir; ls | xargs rm -rf)
   mv $tmp_dir "$bak_dir/$(date -d '1 second ago' +%Y-%m-%d_%H-%M-%S)"
}

replay() {
   datadir="$1"
   binlog_file="$2"
   binlog_pos="$3"
   timestamp="$4"
   sql="$base_dir/replay.sql"
   output2="$base_dir/output2"

   # find related binlog files
   binlogs=`ls $datadir | grep $(echo $binlog_file | awk -F'.' '{print $1}') | grep -v index | sort`
   binlogs=`echo $binlogs | tr ' ' '\n' | grep $binlog_file -A $(echo $binlogs | wc -w)`
   if [ ! -n $binlogs ]; then
      echo "ERR"
      echo "backup binloginfo not match, binlog file not found in current datadir"
      exit 1
   fi

   # retrieve sqls from binlogs
   (cd $datadir; sudo mysqlbinlog $binlogs --start-position=$binlog_pos --stop-datetime=$timestamp) 1>$sql 2>$output2
   if [ -s $output2 ]; then
      echo "ERR"
      echo "retrieve mysql binlog failure"
      cat $output2
      rm -f $sql $output2
      exit 1
   fi

   # import data into mysql
   cat $sql | tr -d '\00' | mysql $mysql_opts &>$output2
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "import from binlog to mysql failure"
      cat $output2
      rm -f $sql $output2
      exit 1
   fi
   rm -f $sql $output2
}

replicate() {
   output="$base_dir/output"

   mysql $mysql_opts &>$output <<eof
STOP SLAVE;
RESET SLAVE;
CHANGE MASTER TO
  MASTER_HOST='$secondary_host',
  MASTER_PORT=$secondary_port,
  MASTER_USER='$secondary_user',
  MASTER_PASSWORD='$secondary_passwd';
START SLAVE;
eof
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "start primary replication failure"
      cat $output
      rm -f $output
      exit 1
   fi
   rm -f $output
}

usage() {
   echo "Usage:"
   echo "  ${0##*/} create <client_addr> <client_port>"
   echo "  ${0##*/} delete <timestamp>"
   echo "  ${0##*/} restore <client_addr> <client_port> <md5> [<timestamp>]"
   echo "  ${0##*/} replay <datadir> <binlog_file> <binlog_pos> <timestamp>"
   echo "  ${0##*/} replicate"
   exit 1
}

[ $# -ge 1 ] || usage
case $1 in
   create)
      [ $# -ge 3 ] || usage
      create $2 $3
      ;;
   delete)
      [ $# -ge 2 ] || usage
      delete $2
      ;;
   restore)
      [ $# -ge 4 ] || usage
      restore $2 $3 $4 $5
      ;;
   replay)
      [ $# -ge 5 ] || usage
      replay $2 $3 $4 $5
      ;;
   replicate)
      [ $# -ge 1 ] || usage
      replicate
      ;;
   *)
      usage
      ;;
esac
exit 0
