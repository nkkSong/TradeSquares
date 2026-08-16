package net.tradesquares.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.tradesquares.data.model.ShopFile;

/**
 * JSON schema 版本迁移骨架。
 * 当前版本 = 1；未来升级结构时在此追加迁移链（借鉴 ViScriptShop 的版本迁移思路）。
 */
public final class ShopMigrator {
    private ShopMigrator() {}

    public static JsonElement migrate(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            return root;
        }
        JsonObject obj = root.getAsJsonObject();
        int schema = obj.has("schema") ? obj.get("schema").getAsInt() : 1;
        if (schema > ShopFile.CURRENT_SCHEMA) {
            throw new IllegalStateException("schema " + schema + " 高于当前支持版本 " + ShopFile.CURRENT_SCHEMA);
        }
        // schema 1：无需迁移。后续版本在此追加迁移链：migrateV1ToV2、migrateV2ToV3 ...
        return obj;
    }
}
