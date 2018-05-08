package cn.portal.esb.coproc.redis;

// Redis操作类
// RedisInfo是配置项JavaBean
// BrpopCallback是循环执行brpop任务的回调接口
// RedisShard是针对单一Redis节点的操作类
// RedisCluster是管理本机房Redis集群的操作类，包括一致性哈希，以及写操作的跨机房同步