/*
 * Copyright (C) 2011 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mylhyl.zxing.client;

enum IntentSource {
    /**
     * 本地app向BS(Barcode Scanner)发起的启动指令
     * 比如在androidtest项目中，利用整合的android-integration对BS发起调用指令：com.google.zxing.client.android.SCAN
     * BS中该启动命令对应的Source类型便是NATIVE_APP_INTENT
     */
    NATIVE_APP_INTENT,

    /**
     * 打开BS的时候传入查询商品的url，与最终扫描到的product id结合进行查询
     * 两种url的形式不同
     */
    PRODUCT_SEARCH_LINK,
    ZXING_LINK,
    /**
     * 直接打开BS
     */
    NONE

}
