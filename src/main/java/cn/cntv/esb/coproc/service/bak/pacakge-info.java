package cn.portal.esb.coproc.service.bak;

// 多个异步服务节点，只有一个是备份节点，BackupServiceLocal是调用脚本执行真正的备份操作
// BackupServiceRemote是对于非备份节点，也提供了备份、恢复操作接口，会将实际的操作动作转发给备份节点进行
// BackupScheduledTask备份定时任务，定期备份、定期归档、定期清理