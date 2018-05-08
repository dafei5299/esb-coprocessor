#!/bin/bash

export LANG=zh_CN.utf8
base_dir=`dirname $(readlink -f $0)`
xtra_opts="--defaults-file=/usr/local/etc/my.cnf --use-memory=1G --rsync"
mysql_script="/etc/init.d/mariadb"
mysql_data="/data/mariadb/datadir/mariadb"
mysql_opts="--defaults-file=/usr/local/etc/my.cnf"
primary_host="192.168.151.158"
primary_port="3306"
primary_user="repl"
primary_passwd="repl"

restore() {
   client="$1 $2"
   md5_1="$3"
   md5="$base_dir/md5"
   tmp_file="$base_dir/backup.tar.gz"
   tmp_dir="$base_dir/tmp"
   output="$base_dir/output"

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

   # start secondary
   binlog_file=`cat $tmp_dir/xtrabackup_binlog_info | awk '{print $1}'`
   binlog_pos=`cat $tmp_dir/xtrabackup_binlog_info | awk '{print $2}'`
   mysql $mysql_opts &>$output <<eof
CHANGE MASTER TO
  MASTER_HOST='$primary_host',
  MASTER_PORT=$primary_port,
  MASTER_USER='$primary_user',
  MASTER_PASSWORD='$primary_passwd',
  MASTER_LOG_FILE='$binlog_file',
  MASTER_LOG_POS=$binlog_pos;
START SLAVE;
eof
   if [ $? -ne 0 ]; then
      echo "ERR"
      echo "start secondary replication failure"
      cat $output
      rm -rf $tmp_dir $output
      exit 1
   fi
   rm -rf $tmp_dir $output

   echo "OK"
   echo "downtime: $downtime"
}

usage() {
   echo "Usage:"
   echo "  ${0##*/} restore <client_addr> <client_port> <md5>"
   exit 1
}

[ $# -ge 1 ] || usage
case $1 in
   restore)
      [ $# -ge 4 ] || usage
      restore $2 $3 $4
      ;;
   *)
      usage
      ;;
esac
exit 0
