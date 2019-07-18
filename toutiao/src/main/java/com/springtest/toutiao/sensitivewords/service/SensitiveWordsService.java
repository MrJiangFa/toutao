package com.springtest.toutiao.sensitivewords.service;


import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.springtest.toutiao.sensitivewords.cache.RedisWordsCache;
import com.springtest.toutiao.sensitivewords.dao.SensitiveWordsDao;
import com.springtest.toutiao.sensitivewords.event.CacheChangedEvent;
import com.springtest.toutiao.sensitivewords.event.CacheChangedEvent.Action;
import com.springtest.toutiao.sensitivewords.event.WordsCacheContext;
import com.springtest.toutiao.sensitivewords.log.ApplicationLogging;
import com.springtest.toutiao.sensitivewords.model.SensitiveWords;
import com.springtest.toutiao.sensitivewords.model.SensitiveWordsType;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.*;



/**
 * 敏感词库服务接口业务实现
 */

public class SensitiveWordsService<T extends SensitiveWords> extends ApplicationLogging {


	private SensitiveWordsDao<T> dao;
	
	public SensitiveWordsService() {
		
		RedisWordsCache.getInstance().setDataSource(this);
	}
	
	public boolean add(T entity) throws Exception {
		logger.debug("添加[敏感词库]数据参数：{}", entity);
		
		validate(entity);

		boolean flag = false;
		try {
			bind(entity);
			checkUnique(entity);
			
			flag = dao.add(entity) > 0;
			
			if (flag) {
				WordsCacheContext.getInstance().dispatchChanged(new CacheChangedEvent(entity, Action.PUT));
			}
        } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        throw e;
        }
		
		logger.debug("添加[敏感词库]数据是否成功：{}", flag);
		return flag;
	}
	
	private void checkUnique(T entity) throws Exception {
		@SuppressWarnings("unchecked")
		T card = dao.get((T) new SensitiveWords(entity.getWord()));
		
		checkState(card == null, "该敏感词：%s 已经存在，不能重复添加", entity.getWord());
	}
	
	public void validate(T entity) throws Exception {
		checkNotNull(entity, "敏感词库对象不能为空");
		
		checkArgument(!Strings.isNullOrEmpty(entity.getWord()), "敏感词不能为空");
		checkNotNull(entity.getType(), "敏感词类型，1：色情，2：政治，3：暴恐，4：民生，5：反动，6：贪腐，7：其他不能为空");
		checkArgument(!Strings.isNullOrEmpty(entity.getCreator()), "创建人不能为空");
		checkArgument(!Strings.isNullOrEmpty(entity.getUpdater()), "更新人不能为空");
	}
	
	public void bind(T entity) throws Exception {
		if (entity.getSensitiveWordsId() == null) {
			long sensitiveWordsId = System.currentTimeMillis();
			entity.setSensitiveWordsId(sensitiveWordsId);
		}

		Date now = new Date();
		entity.setCreateTime(now);
		entity.setUpdateTime(now);
	}
	
	public boolean batch(List<T> entities, SensitiveWordsType type) throws Exception {
		logger.debug("批量添加[敏感词库]数据参数：{}", entities.size());
		
		boolean flag = false;
		try {
			if (entities != null && !entities.isEmpty()) {
				
				List<T> result = Lists.newArrayList();
				
				Iterator<T> iter = entities.iterator();
				while (iter.hasNext()) {
					T entity = iter.next();
					entity.setType(type);
					
					validate(entity);
					bind(entity);
					checkUnique(entity);
				}
				
				int count = dao.batch(result);
				logger.debug("批量入库敏感词库数据：{}", count);
				
				if (count != result.size()) {
					throw new RuntimeException("批量入库敏感词库数据不完整");
				} 
				
				WordsCacheContext.getInstance().dispatchChanged(new CacheChangedEvent(result, Action.PUT_LIST));
			} 
			
			flag = true;
        } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        throw e;
        }
		
		logger.debug("批量添加[敏感词库]数据是否成功：{}", flag);
		return flag;
	}
	
    public boolean edit(T entity) throws Exception {
		logger.debug("修改[敏感词库]数据参数：{}", entity);
		
		if (entity != null) {
			entity.setUpdateTime(new Date());
		}
		
		boolean flag = false;
		try {
			flag = dao.edit(entity) > 0;
			
			if (flag) {
				WordsCacheContext.getInstance().dispatchChanged(new CacheChangedEvent(entity, Action.UPDATE));
			}
        } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        throw e;
        }
		
		logger.debug("修改[敏感词库]数据是否成功：{}", flag);
		return flag;
	}
	
    public boolean remove(T entity) throws Exception {
		logger.debug("删除[敏感词库]数据参数：{}", entity);
		
		boolean flag = false;
		try {
			flag = dao.remove(entity) > 0;
			
			if (flag) {
				WordsCacheContext.getInstance().dispatchChanged(new CacheChangedEvent(entity, Action.REMOVE));
			}
        } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        throw e;
        }
		
		logger.debug("删除[敏感词库]数据是否成功：{}", flag);
		return flag;
	}
	
    public List<T> list(T entity) throws Exception {
		logger.debug("动态查询[敏感词库]数据参数 Entity：{}", entity);
		
		List<T> result = new ArrayList<T>();
		try {
			result = dao.query(entity);
        } catch (Exception e) {
	        logger.error(e.getMessage(), e);
	        throw e;
        }
		
		logger.debug("动态查询[敏感词库]数据结果集：{}", result);
		return result;
	}
}
