// SPDX-FileCopyrightText: 2023 LakeSoul Contributors
//
// SPDX-License-Identifier: Apache-2.0

package com.facebook.presto.benchmark;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Benchmark {
    static String hostname = "mysql";
    //static String hostname = "localhost";
    static String  mysqlUserName = "root";
    static String  mysqlPassword = "root";
    static int mysqlPort = 3306;
    static String  serverTimeZone = "UTC";
    static String prestoHost = "127.0.0.1";
    static String prestoPort = "8080";


    /** for CDC */
    static String  dbName = "test_cdc";
    static boolean verifyCDC = true;

    /** for single test  */
    static boolean singleLakeSoulContrast = false;
    static String  lakeSoulDBName = "flink_sink";
    static String  lakeSoulTableName = "default_init";

    static final String  DEFAULT_INIT_TABLE = "default_init";
    static final String  printLine = " ******** ";
    static final String  splitLine = " --------------------------------------------------------------- ";

    static Connection mysqlCon = null;
    static Connection prestoCon = null;

    /**
     * param example:
     * --mysql.hostname localhost
     * --mysql.database.name default_init
     * --mysql.username root
     * --mysql.password root
     * --mysql.port 3306
     * --server.time.zone UTC
     * --cdc.contract true
     * --single.table.contract false
     * --lakesoul.database.name lakesoul_test
     * --lakesoul.table.name lakesoul_table
     */
    public static void main(String[] args) throws Exception {
        ParametersTool parameter = ParametersTool.fromArgs(args);
        hostname = parameter.get("mysql.hostname", hostname);
        dbName = parameter.get("mysql.database.name", dbName);
        mysqlUserName = parameter.get("mysql.username", mysqlUserName);
        mysqlPassword = parameter.get("mysql.password", mysqlPassword);
        mysqlPort = parameter.getInt("mysql.port", mysqlPort);
        prestoHost = parameter.get("presto.hostname", prestoHost);
        prestoPort = parameter.get("presto.port", prestoPort);
        serverTimeZone = parameter.get("server.time.zone", serverTimeZone);
        verifyCDC = parameter.getBoolean("cdc.contract", true);

        String mysqlUrl = "jdbc:mysql://" + hostname + ":" + mysqlPort + "/" + dbName + "?allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=" + serverTimeZone;
        String prestoUrl = "jdbc:presto://" + prestoHost + ":" + prestoPort + "/lakesoul/";
        mysqlCon = DriverManager.getConnection(mysqlUrl, mysqlUserName, mysqlPassword);
        prestoCon = DriverManager.getConnection(prestoUrl, "test", null);

        singleLakeSoulContrast = parameter.getBoolean("single.table.contract", false);
        if (singleLakeSoulContrast) {
            lakeSoulDBName = parameter.get("lakesoul.database.name", lakeSoulDBName);
            lakeSoulTableName = parameter.get("lakesoul.table.name", lakeSoulTableName);
            mysqlCon.setSchema(lakeSoulDBName);
            prestoCon.setSchema(lakeSoulDBName);
            System.out.println(splitLine);
            System.out.println("verifing single tableName = " + lakeSoulDBName + "." + lakeSoulTableName);
            verifyQuery(dbName, lakeSoulDBName, lakeSoulTableName);
            System.out.println(splitLine);
        }

        if (verifyCDC) {
            mysqlCon.setSchema(dbName);
            prestoCon.setSchema(dbName);
            // query tables in mysql and then verify them in presto
            ResultSet tablesResults = mysqlCon.prepareStatement("show tables").executeQuery();
            while (tablesResults.next()){
                String tableName = tablesResults.getString(1);
                System.out.println(splitLine);
                System.out.println("verifing tableName = " + dbName + "." + tableName);
                verifyQuery(dbName, dbName, tableName);
            }
        }

        mysqlCon.close();
        prestoCon.close();
    }

    @Deprecated
    public static void verifyQueryHard (String table) throws SQLException {
        String sql = "select * from " + table;
        ResultSet res1 = mysqlCon.prepareStatement(sql).executeQuery();
        ResultSet res2 = prestoCon.prepareStatement(sql).executeQuery();
        while (res1.next()){
            if(!res2.next()) {
                throw new RuntimeException("row count do not match");
            }
            int n = res1.getMetaData().getColumnCount();
            for(int i = 1; i <= n; i++){
                Object object1 = res1.getObject(i);
                Object object2 = res2.getObject(i);
                if(object1 == object2 || object1.equals(object2)){
                    continue;
                }
                throw new RuntimeException("not equals: " + object1  + " and " + object2);
            }
        }

        System.out.println("table " + table + " matched");
//        if (table.equals(DEFAULT_INIT_TABLE)) {
//            jdbcDF = changeDF(jdbcDF)
//            lakesoulDF = changeDF(lakesoulDF)
//        }

    }


    public static void verifyQuery (String sourceSchema, String sinkSchema, String table) throws SQLException {
        String sourceSchemaTableName = sourceSchema + "." + table;
        String sinkSchemaTableName = sinkSchema + "." + table;
        String sql1 = String.format("select count(*) from lakesoul.%s", sinkSchemaTableName) ;
        String sql2 = String.format("select count(*) from mysql.%s", sourceSchemaTableName) ;
        String sql3 = String.format("select count(*) from (select * from lakesoul.%s except select * from mysql.%s)", sinkSchemaTableName, sourceSchemaTableName);
        String sql4 = String.format("select count(*) from (select * from mysql.%s except select * from lakesoul.%s)", sourceSchemaTableName, sinkSchemaTableName);
        String sql5 = String.format("select * from lakesoul.%s", sinkSchemaTableName) ;
        String sql6 = String.format("select * from mysql.%s", sourceSchemaTableName) ;
        try {
            int count1 = getCount(prestoCon.prepareStatement(sql1).executeQuery());
            int count2 = getCount(prestoCon.prepareStatement(sql2).executeQuery());
            int count3 = getCount(prestoCon.prepareStatement(sql3).executeQuery());
            int count4 = getCount(prestoCon.prepareStatement(sql4).executeQuery());
            if(count1 == 0 || count2 == 0 || count3 != 0 || count4 != 0){
                System.out.println("count1=" + count1);
                System.out.println("count2=" + count2);
                System.out.println("count3=" + count3);
                System.out.println("count4=" + count4);
                throw new RuntimeException("table " + table + " is not matched");
            }
            System.out.println("table " + table + " matched");
        }catch (Exception e){
            ResultSetPrinter.printResultSet(prestoCon.prepareStatement(sql5).executeQuery());
            ResultSetPrinter.printResultSet(prestoCon.prepareStatement(sql6).executeQuery());
            throw e;
        }
    }

    static int getCount(ResultSet res) throws SQLException {
        if(res.next()){
            return res.getInt(1);
        }
        throw new RuntimeException("resultset has not records");
    }


//    DataFrame changeDF(df: DataFrame) {
//        df.withColumn("col_2", col("col_2").cast("string"))
//                .withColumn("col_3", col("col_3").cast("string"))
//                .withColumn("col_11", col("col_11").cast("string"))
//                .withColumn("col_13", col("col_13").cast("string"))
//                .withColumn("col_20", col("col_20").cast("string"))
//                .withColumn("col_23", col("col_23").cast("string"))
//    }

}


/**
 * 结果集打印机.将结果集中的数据打印成表格.
 */
class ResultSetPrinter {
    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData resultSetMetaData = rs.getMetaData();
        // 获取列数
        int ColumnCount = resultSetMetaData.getColumnCount();
        // 保存当前列最大长度的数组
        int[] columnMaxLengths = new int[ColumnCount];
        // 缓存结果集,结果集可能有序,所以用ArrayList保存变得打乱顺序.
        ArrayList<String[]> results = new ArrayList<>();
        // 按行遍历
        while (rs.next()) {
            // 保存当前行所有列
            String[] columnStr = new String[ColumnCount];
            // 获取属性值.
            for (int i = 0; i < ColumnCount; i++) {
                // 获取一列
                columnStr[i] = rs.getString(i + 1);
                // 计算当前列的最大长度
                columnMaxLengths[i] = Math.max(columnMaxLengths[i], (columnStr[i] == null) ? 0 : columnStr[i].length());
            }
            // 缓存这一行.
            results.add(columnStr);
        }
        printSeparator(columnMaxLengths);
        printColumnName(resultSetMetaData, columnMaxLengths);
        printSeparator(columnMaxLengths);
        // 遍历集合输出结果
        Iterator<String[]> iterator = results.iterator();
        String[] columnStr;
        while (iterator.hasNext()) {
            columnStr = iterator.next();
            for (int i = 0; i < ColumnCount; i++) {
                // System.out.printf("|%" + (columnMaxLengths[i] + 1) + "s", columnStr[i]);
                System.out.printf("|%" + columnMaxLengths[i] + "s", columnStr[i]);
            }
            System.out.println("|");
        }
        printSeparator(columnMaxLengths);
    }

    /**
     * 输出列名.
     *
     * @param resultSetMetaData 结果集的元数据对象.
     * @param columnMaxLengths  每一列最大长度的字符串的长度.
     * @throws SQLException
     */
    private static void printColumnName(ResultSetMetaData resultSetMetaData, int[] columnMaxLengths) throws SQLException {
        int columnCount = resultSetMetaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            // System.out.printf("|%" + (columnMaxLengths[i] + 1) + "s", resultSetMetaData.getColumnName(i + 1));
            System.out.printf("|%" + columnMaxLengths[i] + "s", resultSetMetaData.getColumnName(i + 1));
        }
        System.out.println("|");
    }

    /**
     * 输出分隔符.
     *
     * @param columnMaxLengths 保存结果集中每一列的最长的字符串的长度.
     */
    private static void printSeparator(int[] columnMaxLengths) {
        for (int i = 0; i < columnMaxLengths.length; i++) {
            System.out.print("+");
            // for (int j = 0; j < columnMaxLengths[i] + 1; j++) {
            for (int j = 0; j < columnMaxLengths[i]; j++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }

}

