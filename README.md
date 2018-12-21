# FindMoto
個人大學畢業專題
主要運用iBeacon來做到協尋機車的目的


## 編寫日誌
此表用於紀錄每次提交的內容，方便個人查找

1. 2018/06/05
	* 介面建立
	 
2. 2018/10/13
   * 加入新系統前的預防性存檔
	* 增加註解
	* 改進程式碼
	
3. 2018/11/11
   * 建置資料庫
	- 可搜尋附近iBeacon
	* 可將iBeacon資料寫進資料庫
	
4. 2018/11/12
   * 裝置清單可以看見是否"在偵測範圍內"之圖示
	- 畫面設定成固定保持縱向
	* 減少UIMain.class無用程式碼(請求網路存取權、連結伺服器等)
	- ListItem.class自UIMain獨立出來成為ListItem.java
5. 2018/11/15
   * 裝置清單可以打開查看詳細
	- 開啟詳細後可以使用編輯功能編輯
	* 可以在詳細畫面刪除裝置
	- 裝置清單現在會顯示哪個裝置是預設裝置
	* 主題配色大幅更改、新增自定義外觀
6. 2018/11/21
   * FindActivity.java 初始化設定完成
	- activity_find.xml 畫面建立完成
	* 新增自訂義 Beacon 物件
	- 介面部分微調
	* 從這裡開始使用Google Bluetooth Low Energy Overview
7. 2018/11/22
   * 已可以在find畫面看到距離、訊號強度、方向正確與否的資訊
 	- 使用smoothDist滑順測距結果
	* 在MenuActivity到FindActivity的部分putExtra了預設裝置的name及mac
8. 2018/11/28
   - Find畫面中距離、訊號強度、方向正確與否等完成(改進前次)
   	* progressBar功能完成
   	- 取消smoothDist
   	* 運用distArray[5]去除不穩定的訊號
9. 2018/11/29
   - 新增icon
   	* 加回smoothDist
10. 2018/12/01
   	- 加入儲存現在位置按鈕 以儲存使用者當前位置
11. 2018/12/05
   	- FindActivity加入Google Map功能
   	* 顯示機車位置
   	- 顯示自己位置
11. 2018/12/10
   	- 加入 Widget 桌面小工具
   	* 按下後可以記錄現在位置，讓使用者不用進入主程式也能記錄座標(外觀待改進)
   	- content_start.xml 首頁圖片(logo)更新
12. 2018/12/13
   	- 在開始與矯正精準度頁面新增了目前預設裝置
   	* 調整 activity_correction.xml 與 content_setting.xml 畫面
   	- 加入content_start.xml、content_list.xml、content_setting.xml介面切換時title會跟著切換的功能
	* Beacon 新增屬性 shieldVar 為計算測距時的環境干擾變數
   	- CorrectionActivity.java 初始化設定完成
13.2018/12/22
   	- CorrectionActivity.java 矯正功能完成
   	* 原變數 shieldVar 改成 atOneMeter 並作為"在一公尺處之rssi值"之意思
   	- 資料庫多一欄位 atOneMeter 屬性為integer 為存放"在一公尺處rssi值之絕對值"作為測距使用
	* FindActivity.java 內容待優化及加入使用資料庫內的atOneMeter作為測距基準之功能

