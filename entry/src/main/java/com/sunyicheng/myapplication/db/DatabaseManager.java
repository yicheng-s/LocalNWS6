package com.sunyicheng.myapplication.db;

import com.sunyicheng.myapplication.model.FixedAsset;
import com.sunyicheng.myapplication.model.Product;
import com.sunyicheng.myapplication.model.StockRecord;
import ohos.app.Context;
import ohos.data.DatabaseHelper;
import ohos.data.rdb.RdbOpenCallback;
import ohos.data.rdb.RdbPredicates;
import ohos.data.rdb.RdbStore;
import ohos.data.rdb.StoreConfig;
import ohos.data.rdb.ValuesBucket;
import ohos.data.resultset.ResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库管理器 - 单例模式
 * 管理本地 SQLite 数据库的所有操作
 */
public class DatabaseManager {

    private static final String DB_NAME = "warehouse.db";
    private static final int DB_VERSION = 2;

    private static DatabaseManager instance;
    private RdbStore rdbStore;

    // 表名常量
    private static final String TABLE_PRODUCTS = "products";
    private static final String TABLE_STOCK_RECORDS = "stock_records";
    private static final String TABLE_FIXED_ASSETS = "fixed_assets";

    private DatabaseManager() {}

    /**
     * 获取单例实例
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * 初始化数据库（应用启动时调用一次）
     */
    public void init(Context context) {
        if (rdbStore != null) {
            return;
        }
        StoreConfig config = StoreConfig.newDefaultConfig(DB_NAME);
        RdbOpenCallback callback = new RdbOpenCallback() {
            @Override
            public void onCreate(RdbStore store) {
                createTables(store);
            }

            @Override
            public void onUpgrade(RdbStore store, int oldVersion, int newVersion) {
                if (oldVersion < 2) {
                    store.executeSql("ALTER TABLE " + TABLE_FIXED_ASSETS +
                            " ADD COLUMN status_date TEXT");
                }
            }
        };

        DatabaseHelper helper = new DatabaseHelper(context);
        rdbStore = helper.getRdbStore(config, DB_VERSION, callback);
    }

    /**
     * 创建所有数据表
     */
    private void createTables(RdbStore store) {
        // 商品表
        store.executeSql("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "barcode TEXT UNIQUE, " +
                "category TEXT, " +
                "unit TEXT, " +
                "stock_quantity INTEGER DEFAULT 0, " +
                "description TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT" +
                ")");

        // 出入库记录表
        store.executeSql("CREATE TABLE IF NOT EXISTS " + TABLE_STOCK_RECORDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "product_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "department TEXT, " +
                "person_name TEXT, " +
                "signature_path TEXT, " +
                "remark TEXT, " +
                "created_at TEXT" +
                ")");

        // 固定资产表
        store.executeSql("CREATE TABLE IF NOT EXISTS " + TABLE_FIXED_ASSETS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "barcode TEXT, " +
                "category TEXT, " +
                "location TEXT, " +
                "department TEXT, " +
                "status TEXT DEFAULT '正常', " +
                "quantity INTEGER DEFAULT 1, " +
                "unit TEXT, " +
                "purchase_date TEXT, " +
                "value REAL DEFAULT 0, " +
                "status_date TEXT, " +
                "remark TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT" +
                ")");
    }

    // ======================= 商品管理 =======================

    /**
     * 新增商品
     */
    public long insertProduct(Product product) {
        ValuesBucket values = new ValuesBucket();
        values.putString("name", product.getName());
        values.putString("barcode", product.getBarcode());
        values.putString("category", product.getCategory());
        values.putString("unit", product.getUnit());
        values.putInteger("stock_quantity", product.getStockQuantity());
        values.putString("description", product.getDescription());
        values.putString("created_at", product.getCreatedAt());
        values.putString("updated_at", product.getUpdatedAt());
        return rdbStore.insert(TABLE_PRODUCTS, values);
    }

    /**
     * 更新商品信息
     */
    public int updateProduct(Product product) {
        ValuesBucket values = new ValuesBucket();
        values.putString("name", product.getName());
        values.putString("barcode", product.getBarcode());
        values.putString("category", product.getCategory());
        values.putString("unit", product.getUnit());
        values.putInteger("stock_quantity", product.getStockQuantity());
        values.putString("description", product.getDescription());
        values.putString("updated_at", product.getUpdatedAt());

        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("id", product.getId());
        return rdbStore.update(values, predicates);
    }

    /**
     * 更新商品库存
     */
    public int updateProductStock(int productId, int newStock, String updatedAt) {
        ValuesBucket values = new ValuesBucket();
        values.putInteger("stock_quantity", newStock);
        values.putString("updated_at", updatedAt);

        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("id", productId);
        return rdbStore.update(values, predicates);
    }

    /**
     * 删除商品
     */
    public int deleteProduct(int productId) {
        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("id", productId);
        return rdbStore.delete(predicates);
    }

    /**
     * 根据名称精确查询商品
     */
    public Product getProductByName(String name) {
        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("name", name);
        ResultSet rs = rdbStore.query(predicates, null);
        Product product = null;
        if (rs.goToFirstRow()) {
            product = cursorToProduct(rs);
        }
        rs.close();
        return product;
    }

    /**
     * 根据条码查询商品
     */
    public Product getProductByBarcode(String barcode) {
        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("barcode", barcode);
        ResultSet rs = rdbStore.query(predicates, null);
        Product product = null;
        if (rs.goToFirstRow()) {
            product = cursorToProduct(rs);
        }
        rs.close();
        return product;
    }

    /**
     * 根据ID查询商品
     */
    public Product getProductById(int id) {
        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        predicates.equalTo("id", id);
        ResultSet rs = rdbStore.query(predicates, null);
        Product product = null;
        if (rs.goToFirstRow()) {
            product = cursorToProduct(rs);
        }
        rs.close();
        return product;
    }

    /**
     * 查询所有商品
     */
    public List<Product> getAllProducts() {
        return queryProducts(null, null);
    }

    /**
     * 按分类查询商品
     */
    public List<Product> getProductsByCategory(String category) {
        return queryProducts("category", category);
    }

    /**
     * 搜索商品（按名称或条码模糊匹配）
     */
    public List<Product> searchProducts(String keyword) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE name LIKE ? OR barcode LIKE ?" +
                " ORDER BY updated_at DESC";
        String[] args = new String[]{"%" + keyword + "%", "%" + keyword + "%"};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            products.add(cursorToProduct(rs));
        }
        rs.close();
        return products;
    }

    private List<Product> queryProducts(String column, String value) {
        List<Product> products = new ArrayList<>();
        RdbPredicates predicates = new RdbPredicates(TABLE_PRODUCTS);
        if (column != null && value != null) {
            predicates.equalTo(column, value);
        }
        predicates.orderByDesc("updated_at");
        ResultSet rs = rdbStore.query(predicates, null);
        while (rs.goToNextRow()) {
            products.add(cursorToProduct(rs));
        }
        rs.close();
        return products;
    }

    private Product cursorToProduct(ResultSet rs) {
        Product p = new Product();
        p.setId(rs.getInt(rs.getColumnIndexForName("id")));
        p.setName(rs.getString(rs.getColumnIndexForName("name")));
        p.setBarcode(rs.getString(rs.getColumnIndexForName("barcode")));
        p.setCategory(rs.getString(rs.getColumnIndexForName("category")));
        p.setUnit(rs.getString(rs.getColumnIndexForName("unit")));
        p.setStockQuantity(rs.getInt(rs.getColumnIndexForName("stock_quantity")));
        p.setDescription(rs.getString(rs.getColumnIndexForName("description")));
        p.setCreatedAt(rs.getString(rs.getColumnIndexForName("created_at")));
        p.setUpdatedAt(rs.getString(rs.getColumnIndexForName("updated_at")));
        return p;
    }

    // ======================= 出入库记录 =======================

    /**
     * 新增出入库记录
     */
    public long insertStockRecord(StockRecord record) {
        ValuesBucket values = new ValuesBucket();
        values.putInteger("product_id", record.getProductId());
        values.putString("type", record.getType());
        values.putInteger("quantity", record.getQuantity());
        values.putString("department", record.getDepartment());
        values.putString("person_name", record.getPersonName());
        values.putString("signature_path", record.getSignaturePath());
        values.putString("remark", record.getRemark());
        values.putString("created_at", record.getCreatedAt());
        return rdbStore.insert(TABLE_STOCK_RECORDS, values);
    }

    /**
     * 查询所有出入库记录（关联商品名称）
     */
    public List<StockRecord> getAllStockRecords() {
        return queryStockRecords(null, null, null);
    }

    /**
     * 按类型查询记录
     */
    public List<StockRecord> getStockRecordsByType(String type) {
        return queryStockRecords("r.type", type, null);
    }

    /**
     * 按日期范围查询记录
     */
    public List<StockRecord> getStockRecordsByDateRange(String startDate, String endDate) {
        List<StockRecord> records = new ArrayList<>();
        String sql = "SELECT r.*, p.name AS product_name, p.barcode AS product_barcode " +
                "FROM " + TABLE_STOCK_RECORDS + " r " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON r.product_id = p.id " +
                "WHERE r.created_at >= ? AND r.created_at <= ? " +
                "ORDER BY r.created_at DESC";
        String[] args = new String[]{startDate + " 00:00:00", endDate + " 23:59:59"};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            records.add(cursorToStockRecord(rs));
        }
        rs.close();
        return records;
    }

    /**
     * 搜索记录（按商品名或人员名）
     */
    public List<StockRecord> searchStockRecords(String keyword) {
        List<StockRecord> records = new ArrayList<>();
        String sql = "SELECT r.*, p.name AS product_name, p.barcode AS product_barcode " +
                "FROM " + TABLE_STOCK_RECORDS + " r " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON r.product_id = p.id " +
                "WHERE p.name LIKE ? OR r.person_name LIKE ? OR r.department LIKE ? " +
                "ORDER BY r.created_at DESC";
        String kw = "%" + keyword + "%";
        String[] args = new String[]{kw, kw, kw};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            records.add(cursorToStockRecord(rs));
        }
        rs.close();
        return records;
    }

    /**
     * 根据ID查询记录
     */
    public StockRecord getStockRecordById(int id) {
        String sql = "SELECT r.*, p.name AS product_name, p.barcode AS product_barcode " +
                "FROM " + TABLE_STOCK_RECORDS + " r " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON r.product_id = p.id " +
                "WHERE r.id = ?";
        String[] args = new String[]{String.valueOf(id)};
        ResultSet rs = rdbStore.querySql(sql, args);
        StockRecord record = null;
        if (rs.goToFirstRow()) {
            record = cursorToStockRecord(rs);
        }
        rs.close();
        return record;
    }

    private List<StockRecord> queryStockRecords(String column, String value, String orderBy) {
        List<StockRecord> records = new ArrayList<>();
        String sql = "SELECT r.*, p.name AS product_name, p.barcode AS product_barcode " +
                "FROM " + TABLE_STOCK_RECORDS + " r " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON r.product_id = p.id";
        if (column != null && value != null) {
            sql += " WHERE " + column + " = ?";
        }
        sql += " ORDER BY r.created_at DESC";

        String[] args = (column != null && value != null) ? new String[]{value} : new String[]{};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            records.add(cursorToStockRecord(rs));
        }
        rs.close();
        return records;
    }

    private StockRecord cursorToStockRecord(ResultSet rs) {
        StockRecord r = new StockRecord();
        r.setId(rs.getInt(rs.getColumnIndexForName("id")));
        r.setProductId(rs.getInt(rs.getColumnIndexForName("product_id")));
        r.setType(rs.getString(rs.getColumnIndexForName("type")));
        r.setQuantity(rs.getInt(rs.getColumnIndexForName("quantity")));
        r.setDepartment(rs.getString(rs.getColumnIndexForName("department")));
        r.setPersonName(rs.getString(rs.getColumnIndexForName("person_name")));
        r.setSignaturePath(rs.getString(rs.getColumnIndexForName("signature_path")));
        r.setRemark(rs.getString(rs.getColumnIndexForName("remark")));
        r.setCreatedAt(rs.getString(rs.getColumnIndexForName("created_at")));

        // 关联字段（可能为 null）
        int nameIdx = rs.getColumnIndexForName("product_name");
        if (nameIdx >= 0) {
            r.setProductName(rs.getString(nameIdx));
        }
        int barcodeIdx = rs.getColumnIndexForName("product_barcode");
        if (barcodeIdx >= 0) {
            r.setProductBarcode(rs.getString(barcodeIdx));
        }
        return r;
    }

    // ======================= 固定资产管理 =======================

    /**
     * 新增固定资产
     */
    public long insertFixedAsset(FixedAsset asset) {
        ValuesBucket values = new ValuesBucket();
        values.putString("name", asset.getName());
        values.putString("barcode", asset.getBarcode());
        values.putString("category", asset.getCategory());
        values.putString("location", asset.getLocation());
        values.putString("department", asset.getDepartment());
        values.putString("status", asset.getStatus());
        values.putString("status_date", asset.getStatusDate());
        values.putInteger("quantity", asset.getQuantity());
        values.putString("unit", asset.getUnit());
        values.putString("purchase_date", asset.getPurchaseDate());
        values.putDouble("value", asset.getValue());
        values.putString("remark", asset.getRemark());
        values.putString("created_at", asset.getCreatedAt());
        values.putString("updated_at", asset.getUpdatedAt());
        return rdbStore.insert(TABLE_FIXED_ASSETS, values);
    }

    /**
     * 更新固定资产
     */
    public int updateFixedAsset(FixedAsset asset) {
        ValuesBucket values = new ValuesBucket();
        values.putString("name", asset.getName());
        values.putString("barcode", asset.getBarcode());
        values.putString("category", asset.getCategory());
        values.putString("location", asset.getLocation());
        values.putString("department", asset.getDepartment());
        values.putString("status", asset.getStatus());
        values.putString("status_date", asset.getStatusDate());
        values.putInteger("quantity", asset.getQuantity());
        values.putString("unit", asset.getUnit());
        values.putString("purchase_date", asset.getPurchaseDate());
        values.putDouble("value", asset.getValue());
        values.putString("remark", asset.getRemark());
        values.putString("updated_at", asset.getUpdatedAt());

        RdbPredicates predicates = new RdbPredicates(TABLE_FIXED_ASSETS);
        predicates.equalTo("id", asset.getId());
        return rdbStore.update(values, predicates);
    }

    /**
     * 删除固定资产
     */
    public int deleteFixedAsset(int id) {
        RdbPredicates predicates = new RdbPredicates(TABLE_FIXED_ASSETS);
        predicates.equalTo("id", id);
        return rdbStore.delete(predicates);
    }

    /**
     * 查询所有固定资产
     */
    public List<FixedAsset> getAllFixedAssets() {
        return queryFixedAssets(null, null);
    }

    /**
     * 按分类查询固定资产
     */
    public List<FixedAsset> getFixedAssetsByCategory(String category) {
        return queryFixedAssets("category", category);
    }

    /**
     * 按状态查询固定资产
     */
    public List<FixedAsset> getFixedAssetsByStatus(String status) {
        return queryFixedAssets("status", status);
    }

    /**
     * 搜索固定资产
     */
    public List<FixedAsset> searchFixedAssets(String keyword) {
        List<FixedAsset> assets = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_FIXED_ASSETS +
                " WHERE name LIKE ? OR barcode LIKE ? OR location LIKE ? OR department LIKE ?" +
                " ORDER BY updated_at DESC";
        String kw = "%" + keyword + "%";
        String[] args = new String[]{kw, kw, kw, kw};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            assets.add(cursorToFixedAsset(rs));
        }
        rs.close();
        return assets;
    }

    /**
     * 根据ID查询固定资产
     */
    public FixedAsset getFixedAssetById(int id) {
        RdbPredicates predicates = new RdbPredicates(TABLE_FIXED_ASSETS);
        predicates.equalTo("id", id);
        ResultSet rs = rdbStore.query(predicates, null);
        FixedAsset asset = null;
        if (rs.goToFirstRow()) {
            asset = cursorToFixedAsset(rs);
        }
        rs.close();
        return asset;
    }

    private List<FixedAsset> queryFixedAssets(String column, String value) {
        List<FixedAsset> assets = new ArrayList<>();
        RdbPredicates predicates = new RdbPredicates(TABLE_FIXED_ASSETS);
        if (column != null && value != null) {
            predicates.equalTo(column, value);
        }
        predicates.orderByDesc("updated_at");
        ResultSet rs = rdbStore.query(predicates, null);
        while (rs.goToNextRow()) {
            assets.add(cursorToFixedAsset(rs));
        }
        rs.close();
        return assets;
    }

    private FixedAsset cursorToFixedAsset(ResultSet rs) {
        FixedAsset a = new FixedAsset();
        a.setId(rs.getInt(rs.getColumnIndexForName("id")));
        a.setName(rs.getString(rs.getColumnIndexForName("name")));
        a.setBarcode(rs.getString(rs.getColumnIndexForName("barcode")));
        a.setCategory(rs.getString(rs.getColumnIndexForName("category")));
        a.setLocation(rs.getString(rs.getColumnIndexForName("location")));
        a.setDepartment(rs.getString(rs.getColumnIndexForName("department")));
        a.setStatus(rs.getString(rs.getColumnIndexForName("status")));
        a.setQuantity(rs.getInt(rs.getColumnIndexForName("quantity")));
        a.setUnit(rs.getString(rs.getColumnIndexForName("unit")));
        a.setPurchaseDate(rs.getString(rs.getColumnIndexForName("purchase_date")));
        a.setValue(rs.getDouble(rs.getColumnIndexForName("value")));
        a.setRemark(rs.getString(rs.getColumnIndexForName("remark")));
        a.setStatusDate(rs.getString(rs.getColumnIndexForName("status_date")));
        a.setCreatedAt(rs.getString(rs.getColumnIndexForName("created_at")));
        a.setUpdatedAt(rs.getString(rs.getColumnIndexForName("updated_at")));
        return a;
    }

    // ======================= 统计查询 =======================

    /**
     * 获取库存统计摘要
     * 返回格式: [商品总数, 库存总量, 库存不足(低于10)的商品数]
     */
    public int[] getProductStatistics() {
        int[] stats = new int[3];
        ResultSet rs = rdbStore.querySql("SELECT COUNT(*) AS cnt FROM " + TABLE_PRODUCTS, null);
        if (rs.goToFirstRow()) {
            stats[0] = rs.getInt(rs.getColumnIndexForName("cnt"));
        }
        rs.close();

        rs = rdbStore.querySql("SELECT SUM(stock_quantity) AS total FROM " + TABLE_PRODUCTS, null);
        if (rs.goToFirstRow()) {
            stats[1] = rs.getInt(rs.getColumnIndexForName("total"));
        }
        rs.close();

        rs = rdbStore.querySql("SELECT COUNT(*) AS low FROM " + TABLE_PRODUCTS +
                " WHERE stock_quantity < 10", null);
        if (rs.goToFirstRow()) {
            stats[2] = rs.getInt(rs.getColumnIndexForName("low"));
        }
        rs.close();

        return stats;
    }

    /**
     * 获取出入库统计
     * 返回格式: [入库总次数, 入库总数量, 出库总次数, 出库总数量]
     */
    public int[] getStockRecordStatistics() {
        int[] stats = new int[4];
        ResultSet rs = rdbStore.querySql(
                "SELECT type, COUNT(*) AS cnt, SUM(quantity) AS total FROM " +
                        TABLE_STOCK_RECORDS + " GROUP BY type", null);
        while (rs.goToNextRow()) {
            String type = rs.getString(rs.getColumnIndexForName("type"));
            int cnt = rs.getInt(rs.getColumnIndexForName("cnt"));
            int total = rs.getInt(rs.getColumnIndexForName("total"));
            if ("in".equals(type)) {
                stats[0] = cnt;
                stats[1] = total;
            } else {
                stats[2] = cnt;
                stats[3] = total;
            }
        }
        rs.close();
        return stats;
    }

    /**
     * 获取固定资产统计
     * 返回格式: [资产总数, 正常数量, 维修数量, 报废数量, 总价值]
     */
    public Object[] getFixedAssetStatistics() {
        Object[] stats = new Object[5];
        stats[4] = 0.0;

        ResultSet rs = rdbStore.querySql(
                "SELECT COUNT(*) AS cnt FROM " + TABLE_FIXED_ASSETS, null);
        if (rs.goToFirstRow()) {
            stats[0] = rs.getInt(rs.getColumnIndexForName("cnt"));
        }
        rs.close();

        rs = rdbStore.querySql(
                "SELECT status, COUNT(*) AS cnt FROM " + TABLE_FIXED_ASSETS +
                        " GROUP BY status", null);
        while (rs.goToNextRow()) {
            String status = rs.getString(rs.getColumnIndexForName("status"));
            int cnt = rs.getInt(rs.getColumnIndexForName("cnt"));
            if ("正常".equals(status)) {
                stats[1] = cnt;
            } else if ("维修".equals(status)) {
                stats[2] = cnt;
            } else if ("报废".equals(status)) {
                stats[3] = cnt;
            }
        }
        rs.close();

        rs = rdbStore.querySql(
                "SELECT SUM(value * quantity) AS total_value FROM " + TABLE_FIXED_ASSETS, null);
        if (rs.goToFirstRow()) {
            stats[4] = rs.getDouble(rs.getColumnIndexForName("total_value"));
        }
        rs.close();

        return stats;
    }

    /**
     * 获取最近 N 条出入库记录
     */
    public List<StockRecord> getRecentRecords(int limit) {
        List<StockRecord> records = new ArrayList<>();
        String sql = "SELECT r.*, p.name AS product_name, p.barcode AS product_barcode " +
                "FROM " + TABLE_STOCK_RECORDS + " r " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON r.product_id = p.id " +
                "ORDER BY r.created_at DESC LIMIT ?";
        String[] args = new String[]{String.valueOf(limit)};
        ResultSet rs = rdbStore.querySql(sql, args);
        while (rs.goToNextRow()) {
            records.add(cursorToStockRecord(rs));
        }
        rs.close();
        return records;
    }

    /**
     * 按分类统计商品库存
     */
    public List<String[]> getStockByCategory() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT category, COUNT(*) AS cnt, SUM(stock_quantity) AS total " +
                "FROM " + TABLE_PRODUCTS + " GROUP BY category ORDER BY total DESC";
        ResultSet rs = rdbStore.querySql(sql, null);
        while (rs.goToNextRow()) {
            String cat = rs.getString(rs.getColumnIndexForName("category"));
            int cnt = rs.getInt(rs.getColumnIndexForName("cnt"));
            int total = rs.getInt(rs.getColumnIndexForName("total"));
            result.add(new String[]{cat != null ? cat : "未分类", String.valueOf(cnt), String.valueOf(total)});
        }
        rs.close();
        return result;
    }

    /**
     * 关闭数据库（应用退出时调用）
     */
    public void close() {
        if (rdbStore != null) {
            rdbStore.close();
            rdbStore = null;
        }
    }
}
