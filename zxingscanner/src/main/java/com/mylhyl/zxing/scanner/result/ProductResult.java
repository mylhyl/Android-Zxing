package com.mylhyl.zxing.scanner.result;

import com.google.zxing.client.result.ProductParsedResult;

/**
 * Created by hupei on 2016/8/12.
 */
public class ProductResult extends Result {
    private final String productID;
    private final String normalizedProductID;

    public ProductResult(ProductParsedResult productParsedResult) {
        this.productID = productParsedResult.getProductID();
        this.normalizedProductID = productParsedResult.getNormalizedProductID();
    }

    public String getProductID() {
        return productID;
    }

    public String getNormalizedProductID() {
        return normalizedProductID;
    }
}
