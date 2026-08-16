package net.tradesquares.data;

import net.tradesquares.data.model.ShopFile;

import java.util.List;

/** 一次商店加载的结果：成功列表 + 逐文件错误。 */
public record ShopLoadResult(List<ShopFile> shops, List<String> errors) {
}
