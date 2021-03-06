package com.pkast.bbs.impl;

import com.pkast.bbs.dao.BbsDao;
import com.pkast.bbs.itf.BbsBusiness;
import com.pkast.bbs.module.*;
import com.pkast.bbs.util.BbsDbUtil;
import com.pkast.utils.CheckValidUtil;
import com.pkast.utils.CollectionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BbsBusinessImpl implements BbsBusiness {
    private static final Logger LOGGER = LoggerFactory.getLogger(BbsBusinessImpl.class);

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int DEFAULT_PAGE_IDX = 1;

    @Autowired
    private BbsDao bbsDao;

    public List<List<BbsItem>> getBbs(int pageIdx, int pageSize, String xiaoquId, String bbsType) {
        String dbName = BbsDbUtil.getDbName(xiaoquId);
        if (dbName == null) {
            return null;
        }

        pageSize = DEFAULT_PAGE_SIZE;
        if (pageIdx < 0) {
            pageIdx = DEFAULT_PAGE_IDX;
        }
        int offset = (pageIdx - 1) * pageSize;
        List<BbsDbModel> bbsModels = bbsDao.getBbsItem(dbName, bbsType, offset, pageSize);
        return CollectionUtil.isEmpty(bbsModels) ? null :
                bbsModels.stream()
                        .map(bbsModel -> {
                            PublishBbsBase publishBbs = PublishBbsFactory.makePublishBbs(bbsModel.getType(), bbsModel.getPropertyMap());
                            BbsItem bbsItem = publishBbs == null ? null : publishBbs.toBbsItem();
                            if(bbsItem == null){
                                return null;
                            }
                            bbsItem.setBindtap("call_" + publishBbs.getId());
                            return Arrays.asList(bbsItem);
                        })
                        .filter(item -> item != null)
                        .collect(Collectors.toList());
    }

    @Override
    public CheckValidUtil.CHECK_INVALID_CODE addBbs(PublishBbsRaw bbsRaw) {
        PublishBbsBase publishBbs = PublishBbsFactory.makePublishBbs(bbsRaw.getType(), bbsRaw.getProperties());
        if(publishBbs == null){
            return CheckValidUtil.CHECK_INVALID_CODE.BBS_TYPE_INVALID;
        }
        CheckValidUtil.CHECK_INVALID_CODE checkValidCode = publishBbs.checkValid();
        if(CheckValidUtil.CHECK_INVALID_CODE.VALID_OK != checkValidCode){
            return checkValidCode;
        }

        BbsDbModel dbModule = new BbsDbModel(bbsRaw.getType(), bbsRaw.getProperties());
        String dbName = BbsDbUtil.getDbName(dbModule.getXiaoquId());
        if(StringUtils.isEmpty(dbName)){
            LOGGER.info("get no dbname .{}", dbModule.getXiaoquId());
            return CheckValidUtil.CHECK_INVALID_CODE.XIAOQU_ID_INVALID;
        }
        bbsDao.addBbs(dbName, dbModule);
        return CheckValidUtil.CHECK_INVALID_CODE.VALID_OK;
    }

    public void delBbs(String xiaoquId, String bbsId) {
        String dbName = BbsDbUtil.getDbName(xiaoquId);
        if (StringUtils.isEmpty(dbName)) {
            LOGGER.info("del bbs err.{}", xiaoquId);
            return;
        }
        bbsDao.deleteBbs(dbName, bbsId);
    }
}
