package cn.portal.esb.coproc.service;

// 配置中心
// ConfigService负责从mysql中提取数据，向zk中更新配置信息，或者忽略数据库，直接更新zk
// ConfigData负责从zk中注册观察者，提取最新配置，并提供get接口，让其他业务功能模块可以获取最新配置信息
// 为了防止get接口获取的JavaBean直接被业务修改，获取的JavaBean是clone出来的副本

// DataCenterAware启动时获取本实例所在机房id信息，以及MySQL备份节点配置，提供get方法让其他业务模块可以获取该信息
// NgxCacheLocalCluster注册zk，获取本机房的转发服务节点信息，并根据zk中的配置信息更新转发服务的推送配置和操作
// PeriodTask周期性任务基类，包括领取licence