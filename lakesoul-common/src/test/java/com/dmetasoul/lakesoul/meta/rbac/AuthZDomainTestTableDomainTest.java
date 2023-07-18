// SPDX-FileCopyrightText: 2023 LakeSoul Contributors
//
// SPDX-License-Identifier: Apache-2.0

package com.dmetasoul.lakesoul.meta.rbac;

import com.alibaba.fastjson.JSONObject;
import com.dmetasoul.lakesoul.meta.GlobalConfig;
import com.dmetasoul.lakesoul.meta.entity.TableInfo;
import org.junit.After;
import org.junit.Test;

public class AuthZDomainTestTableDomainTest extends AuthZDomainTest{

    protected final String TEST_NAME_SPACE = "authz_test_name_space_test_table_with_domain";
    protected final String TEST_TABLE_ID = "148BD987-965B-5A63-41F1-6146F7C5DA62";

    protected final String TEST_TABLE_NAME = "148BD987-965B-5A63-41F1-6146F7C5DA62";

    @Test
    public void test(){
        GlobalConfig.get().setAuthZEnabled(true);
        AuthZContext.getInstance().setDomain(TEST_DOMAIN);
        dbManager.createNewTable(
                TEST_TABLE_ID,
                TEST_NAME_SPACE,
                TEST_TABLE_NAME,
                TEST_TABLE_PATH,
                "schema",
                new JSONObject(),
                "");
        TableInfo tableInfoByTableId = dbManager.getTableInfoByTableId(TEST_TABLE_ID);
        assert tableInfoByTableId.getDomain().equals(TEST_DOMAIN);

    }

    @After
    public void clean(){
        dbManager.deleteTableInfo(TEST_TABLE_PATH, TEST_TABLE_ID, TEST_NAME_SPACE);
    }
}
