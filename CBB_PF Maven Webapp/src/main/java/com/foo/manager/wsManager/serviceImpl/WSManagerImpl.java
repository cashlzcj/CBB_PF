package com.foo.manager.wsManager.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.dom4j.util.XMLErrorHandler;
import org.xml.sax.SAXException;

import com.foo.common.CommonDefine;
import com.foo.manager.wsManager.service.WSManagerService;
import com.foo.util.CommonUtil;
import com.foo.util.XmlUtil;

@WebService
public class WSManagerImpl extends WSManagerService{
	
	private static String FILE_TYPE_SNT101 = "SNT101";
	private static String FILE_TYPE_SNT102 = "SNT102";
	
	private static String CUSTOM_CODE = "2308";
	

	@Override
	public String ParseXml(String xmlString) {
		
		String xmlReturnString = "";
		
		//截取fileType
		String fileType = xmlString.substring(
				xmlString.indexOf("<fileType>") + 10,
				xmlString.indexOf("</fileType>"));
		
		//验证xml
		String returnInfo = validateXml(xmlString,fileType);
		
		//验证不通过，返回错误信息
		if(!returnInfo.isEmpty()){
			Map data = new HashMap();
			//返回运单状态
			Map content = generateReturnMap(data,"",returnInfo,CommonDefine.FAILED);
			xmlReturnString = XmlUtil.generalReceiptXml_WS(FILE_TYPE_SNT102,content);
			return xmlReturnString;
		}
		
		//解析数据，获取fileType，xml
		Map<String,String> result = XmlUtil.parseXmlRoot_WS(xmlString);
		
		String xmlContent = result.get("xml").toString();

		//处理报文
		xmlReturnString = handleXml(fileType,xmlContent);

		return xmlReturnString;
	}
	
	//处理具体报文方法
	private String handleXml(String fileType,String xmlString){
		
		String xmlReturnString = "";
		
		if(FILE_TYPE_SNT101.equals(fileType)){
			xmlReturnString = handleXml_SNT101(xmlString);
		}
		
		
		return xmlReturnString;
	}
	
	
	//处理SNT101报文
	private String handleXml_SNT101(String xmlString){
		
		String xmlReturnString = "";
		
		Map<String,Object> data =  XmlUtil.parseXmlSNT101_WS(xmlString);
		
		Map head = (Map) data.get("OrderHead");
		
		List<Map> OrderList = (List<Map>) data.get("OrderList");
		
		//检查订单是否在数据库中存在
		String OrderNo = head.get("OrderNo").toString();
		int count = commonManagerMapper.selectTableListCountByCol("t_nj_orders", "ORDER_NO", OrderNo);
		
		if(count == 0){
			//生成订单数据
			generateOrderDataToDb(head,OrderList);
			//生成运单数据
			String SNTNo = generateLogisticsDataToDb(head);
			
			//返回运单状态
			Map content = generateReturnMap(head,SNTNo,"",CommonDefine.SUCCESS);
			
			xmlReturnString = XmlUtil.generalReceiptXml_WS(FILE_TYPE_SNT102,content);
		}else{
			String SNTNo = getSntNo(OrderNo);
			//返回运单状态
			Map content = generateReturnMap(head,SNTNo,"",CommonDefine.SUCCESS);
			
			xmlReturnString = XmlUtil.generalReceiptXml_WS(FILE_TYPE_SNT102,content);
		}

		return xmlReturnString;
	}
	
	//组织返回数据
	private Map generateReturnMap(Map head,String SNTNo,String returnInfo,int flag){

		String currentTime = new SimpleDateFormat(
				CommonDefine.RETRIEVAL_TIME_FORMAT).format(new Date());
		
		Map content = new LinkedHashMap();
		content.put("EbpCode", head.get("EbpCode"));
		content.put("EbcCode", head.get("EbcCode"));
		content.put("OrderNo", head.get("OrderNo"));
		content.put("returnStatus", flag);
		content.put("returnTime", currentTime);
		content.put("returnInfo", returnInfo);
		content.put("SNTNo", SNTNo);
		
		return content;
	}
	
	//获取中外运订单编号
	private String getSntNo(String OrderNo){
		String SNTNo = "";
		//获取SNTNo
		List logisticsDataList = commonManagerMapper
				.selectTableListByCol("t_nj_logistics", "ORDER_NO",
						OrderNo, null, null);
		
		if(logisticsDataList!=null && logisticsDataList.size()>0){
			Map logisticsData = (Map)logisticsDataList.get(0);
			if(logisticsData.get("LOGISTICS_ID") !=null){
				SNTNo = logisticsData.get("LOGISTICS_ID").toString();
			}
		}
		return SNTNo;
	}
	
	//在数据库中插入订单数据
	private void generateOrderDataToDb(Map head,List<Map> data){
		
		//在head中取需要的列名
		String[] needColumn = new String[]{
				"EBP_CODE","EBC_CODE","ORDER_TYPE","ORDER_NO","GOODS_VALUE",
				"TAX_FEE","FREIGHT","CURRENCY","NOTE"
		};
		
		String tableName=T_NJ_ORDERS;
		String uniqueCol="ORDER_NO";
		String primaryCol="ORDERS_ID";
		
		//获取联系人Id
		Object consigneeId = handleAddress(head);
		//变换列名
		head = changeDbColumn(head);
		
		Map newHead = new HashMap();
		for(String column:needColumn){
			newHead.put(column, head.get(column));
		}

		//设置额外列
		newHead.put("CONSIGNEE_ID", consigneeId);
		//生成guid  CUSTOM_CODE固定2308
		newHead.put("CUSTOM_CODE", CUSTOM_CODE);
		newHead.put("RECEIVER_ID", CUSTOM_CODE);
		newHead.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB301,head.get("EBC_CODE").toString(),CUSTOM_CODE));
		newHead.put("CREAT_TIME", new Date());
		
		Map primary=new HashMap();
		primary.put(primaryCol, null);
		
		commonManagerMapper.insertTableByNVList(tableName,
				new ArrayList<String>(newHead.keySet()), 
				new ArrayList<Object>(newHead.values()),
				primary);
		
		//转换列名
		data = changeDbColumn(data);
		Object primaryId=primary.get("primaryId");
		setGoodsList(data,head.get("ORDER_NO").toString(),primaryId);
		
	}
	
	
	//在数据库中插入运单数据，返回id
	private String generateLogisticsDataToDb(Map head){
		
		//在head中取需要的列名
		String[] needColumn = new String[]{
				"ORDER_NO","INSURE_FEE","NET_WEIGHT","GOODS_INFO","FREIGHT"
		};
		String tableName=T_NJ_LOGISTICS;
		String uniqueCol="LOGISTICS_NO";
		String primaryCol="LOGISTICS_ID";
		//变换列名
		head = changeDbColumn(head);
		
		//特殊字段
		if(head.containsKey("NET_WEIGHT_SP")){
			head.put("NET_WEIGHT", head.get("NET_WEIGHT_SP"));
		}
		
		Map newHead = new HashMap();
		for(String column:needColumn){
			newHead.put(column, head.get(column));
		}
		
		// 设置空id
		newHead.put(primaryCol, null);
		
		// 设置额外列
		newHead.put("CUSTOM_CODE", CUSTOM_CODE);
		newHead.put("RECEIVER_ID", CUSTOM_CODE);
		newHead.put("GUID", CommonUtil.generalGuid4NJ(CommonDefine.CEB501,head.get("EBC_CODE").toString(),CUSTOM_CODE));
		// 设置创建时间
		newHead.put("CREAT_TIME", new Date());
		
		Map primary=new HashMap();
		primary.put("primaryId", null);

		//插入数据
		commonManagerMapper.insertTableByNVList(tableName,
				new ArrayList<String>(newHead.keySet()), 
				new ArrayList<Object>(newHead.values()),
				primary);
		
		return primary.get("primaryId").toString();
		
	}
	
	//地址处理
	private Object handleAddress(Map head){
		
		Object id = null;
		
		List<String> colNames=new ArrayList<String>();
		colNames.add("CODE");
		colNames.add("NAME");
		colNames.add("TEL");
		colNames.add("COUNTRY");
		colNames.add("PROVINCE");
		colNames.add("CITY");
		colNames.add("DISTRICT");
		colNames.add("SPECIFIC_ADDRESS");
		List<Object> colValues=new ArrayList<Object>();
		colValues.add(head.get("ConsigneeCode"));
		colValues.add(head.get("Consignee"));
		colValues.add(head.get("ConsigneeTelephone"));
		colValues.add(head.get("ConsigneeCountry"));
		colValues.add(head.get("ConsigneeProvince"));
		colValues.add(head.get("ConsigneeCity"));
		colValues.add(head.get("ConsigneeDistrict"));
		colValues.add(head.get("ConsigneeAddress"));
		
		//查找t_contact表，是否是已存在的地址
		List<Map<String,Object>> data = commonManagerMapper.selectTableListByNVList("t_contact", colNames, colValues, null, null);
		
		if(data.size()>0){
			id = data.get(0).get("CONTACT_ID");
		}else{
			//插入联系人数据
			Map primary=new HashMap();
			primary.put("primaryId", null);
			
			String address = head.get("PROVINCE") + "_" + head.get("CITY")
					+ "_" + head.get("DISTRICT") + "_"
					+ head.get("SPECIFIC_ADDRESS");
			
			Map contactData = new HashMap();
			
			for(int i = 0;i<colNames.size();i++){
				contactData.put(colNames.get(i), colValues.get(i));
			}
			contactData.put("ADDRESS", address);
			
			commonManagerMapper.insertTableByNVList("T_CONTACT",
					new ArrayList<String>(contactData.keySet()), 
					new ArrayList<Object>(contactData.values()),
					primary);
			
			Object primaryId=primary.get("primaryId");
			id = primaryId;
			
		}
		return id;
	}
	
	
	//插入订单详细信息数据
	private void setGoodsList(List<Map> GOODSList,Object orderNo, Object ordersId){
		commonManagerMapper.delTableById(T_NJ_ORDER_DETAIL, "ORDERS_ID", ordersId);
		Date createTime=new Date();
		for(int i=0;i<GOODSList.size();i++){
//			GOODSList.get(i).put("PRICE", GOODSList.get(i).get("ONE_PRICE"));
//			GOODSList.get(i).put("NOTE", GOODSList.get(i).get("NOTE_OD"));
			Map good=new HashMap();
			good.put("ORDERS_ID", ordersId);
			good.put("ORDER_NO", orderNo);
			good.put("GNUM", i+1);
			good.put("CREAT_TIME", createTime);
			String[] copyColumns=new String[]{"ITEM_NO","QTY","PRICE","TOTAL","NOTE"};
			for(String col:copyColumns){
				good.put(col, GOODSList.get(i).get(col));
			}
			Map primary=new HashMap();
			primary.put("primaryId", null);
			commonManagerMapper.insertTableByNVList(T_NJ_ORDER_DETAIL,
					new ArrayList<String>(good.keySet()), 
					new ArrayList<Object>(good.values()),
					primary);
		}
	}

	
	//校验xml文件
	public String validateXml(String sourceString,String fileType) {

		String result = "";
		
		if(fileType == null || fileType.isEmpty()){
			result = "fileType不正确！";
			return result;
		}
		try {
			// 建立schema工厂
			SchemaFactory schemaFactory = SchemaFactory
					.newInstance("http://www.w3.org/2001/XMLSchema");
			// 建立验证文档文件对象，利用此文件对象所封装的文件进行schema验证
			Source sourceSchema = new StreamSource(Thread
					.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(
							"xmlDataSource/"+fileType+".xsd"));
			// 利用schema工厂，接收验证文档文件对象生成Schema对象
			Schema schema = schemaFactory.newSchema(sourceSchema);
			// 通过Schema产生针对于此Schema的验证器，利用schenaFile进行验证
			Validator validator = schema.newValidator();
//			 // 创建默认的XML错误处理器
//	          XMLErrorHandler errorHandler = new XMLErrorHandler();
//	          validator.setErrorHandler(errorHandler);
			// 得到验证的数据源
			Source sourceFile = new StreamSource(new  ByteArrayInputStream(sourceString.getBytes("UTF-8")));
			// 开始验证，成功输出success!!!，失败输出fail
			validator.validate(sourceFile);

		} catch (SAXException e) {
			result = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}