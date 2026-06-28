package edu.gkg;

import edu.gkg.common.DbHelper;

public class App {
    public static void main(String[] args) {
        System.out.println("🚀 GKG 全球新闻语义分析平台启动中...");

        try {
            DbHelper.getConnection();
            System.out.println("✅ 数据库已就绪");
        } catch (Exception e) {
            System.out.println("数据库连接失败: " + e.getMessage());
        }

        System.out.println("当前只做基础设施验证，UI后续由A同学开发");
    }
}