package cn.portal.esb.coproc.service.stat;

// 统计信息运算相关服务
// ExtractTask通过redis消息队列，提取消息，预处理，放入timeline中
// TransformTask，核心计算任务：1.按各个维度计算汇总值；2.检查是否过载；3.检查是否超过流量限制
// LoadTask，入库
// MergeTask，合并Redis缓存中的汇总信息，在更粗的粒度上汇总，减小内存占用，避免redis占用内存无限增大
// MergeDbTask，基本同上，不过合并的是mysql表中的数据，把日表数据合并到年表中