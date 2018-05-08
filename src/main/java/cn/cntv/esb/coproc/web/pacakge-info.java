package cn.portal.esb.coproc.web;

// 接口层
// /config/system/{id} 根据system id从mysql读，更新到zk中
// /config/group/{id} 根据group id从mysql读，更新到zk中
// /config/api/{id} 根据api id从mysql读，更新到zk中
// /config/node/{id} 根据node id从mysql读，更新到zk中
// /config/latency/{id} 根据latency id从mysql读，更新到zk中
// /config/source/{id} 根据source id从mysql读，更新到zk中
// /config/ngx/{id} 根据ngx id从mysql读，更新到zk中
// /config/sync 强制重新推送到转发服务
// /config/fsync 强制刷新所有数据，从数据库读取，再写到zk中
// /data下的接口是调试用，查看内存中的数据状态
// /data/system
// /data/system/{id}
// /data/group
// /data/group/{id}
// /data/api
// /data/api/{id}
// /data/node
// /data/node/{id}
// /data/source
// /data/source/{id}
// /data/ngx
// /data/ngx/{id}
// /admin/state 查看状态，是Leader还是Follower
// /admin/merge/{day} 指定压缩某一天的日表数据到年表
// /admin/timeline 查看所有mysql备份点
// /admin/backup 进行一次增量备份
// /admin/async/backup 异步执行增量备份
// /admin/delete/{timestamp} 删除某时间点的备份
// /admin/checkpoint/{timestamp} 归档某时间点的备份
// /admin/async/checkpoint/{timestamp} 异步归档
// /admin/restore/{timestamp} 还原到某时间点（需要有备份点存在）
// /admin/async/restore/{timestamp} 异步还原
// /admin/time_machine/{timestamp} 还原到任意时间点
// /admin/async/time_machine/{timestamp} 异步还原
// /log/del/{day}/{filename:.+} 删除某日某日志文件
// /log/tar/{day} 归档某日日志
