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
