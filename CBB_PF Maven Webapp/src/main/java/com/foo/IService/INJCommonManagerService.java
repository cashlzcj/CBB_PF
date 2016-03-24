package com.foo.IService;

import java.util.Map;

import com.foo.common.CommonException;

public interface INJCommonManagerService {
	
	/**
	 * @param params: Integer start, Integer limit, ...
	 * @return Map.key: Integer total, List<Map<String,Object>> rows
	 * @throws CommonException
	 */
	public Map<String,Object> getAllSkus(Map<String,Object> params) throws CommonException;

	/**
	 * @param params: Integer SKU_ID, ...
	 * @throws CommonException
	 */
	public void delSku(Map<String,Object> params) throws CommonException;
	
	/**
	 * @param params: Integer SKU_ID, ...
	 * @throws CommonException
	 */
	public void getReceipt(Map<String,Object> params) throws CommonException;

	/**
	 * @param sku
	 * @throws CommonException
	 */
	public void setSku(Map<String,Object> sku,boolean statusOnly) throws CommonException;

	/**
	 * @param sku
	 * @throws CommonException
	 */
	public void addSku(Map<String,Object> sku) throws CommonException;
	
	
	/**
	 * @param order
	 * @throws CommonException
	 */
	public void setOrder(Map<String,Object> order,boolean statusOnly) throws CommonException;
	/**
	 * @param order
	 * @throws CommonException
	 */
	public void addOrder(Map<String,Object> order) throws CommonException;
	/**
	 * @param params: Integer start, Integer limit, ...
	 * @return Map.key: Integer total, List<Map<String,Object>> rows
	 * @throws CommonException
	 */
	public Map<String,Object> getAllOrders(Map<String,Object> params) throws CommonException;
	/**
	 * @param params: Integer SKU_ID, ...
	 * @throws CommonException
	 */
	public void delOrder(Map<String,Object> params) throws CommonException;
	
	/**
	 * @param params: Integer start, Integer limit, ...
	 * @return Map.key: Integer total, List<Map<String,Object>> rows
	 * @throws CommonException
	 */
	public Map<String,Object> getAllLogisticses(Map<String,Object> params) throws CommonException;
	/**
	 * @param params: Integer LOGISTICS_ID, ...
	 * @throws CommonException
	 */
	public void delLogistics(Map<String,Object> params) throws CommonException;
	/**
	 * @param logistics
	 * @throws CommonException
	 */
	public void setLogistics(Map<String,Object> logistics,boolean statusOnly) throws CommonException;
	/**
	 * @param logistics
	 * @throws CommonException
	 */
	public void addLogistics(Map<String,Object> logistics) throws CommonException;
	
	/**
	 * @param params: Integer start, Integer limit, ...
	 * @return Map.key: Integer total, List<Map<String,Object>> rows
	 * @throws CommonException
	 */
	public Map<String,Object> getAllInventorys(Map<String,Object> params) throws CommonException;
	/**
	 * @param params: Integer INVENTORY_ID, ...
	 * @throws CommonException
	 */
	public void delInventory(Map<String,Object> params) throws CommonException;
	/**
	 * @param inventory
	 * @throws CommonException
	 */
	public void setInventory(Map<String,Object> inventory,boolean statusOnly) throws CommonException;
	/**
	 * @param inventory
	 * @throws CommonException
	 */
	public void addInventory(Map<String,Object> inventory) throws CommonException;
	
	/**
	 * @param params
	 * @throws CommonException
	 */
	public void batchSubmit_LOGISTICS(Map<String,Object> params) throws CommonException;
	
}
