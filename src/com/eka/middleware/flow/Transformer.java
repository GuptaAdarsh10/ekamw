package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Transformer {
	private boolean disabled=false;
	private String condition;
	private String label;
	private boolean evaluateCondition;
	private List<JsonOp> ops;
	private JsonObject transformer;
	private JsonObject data;
	private String comment;
	private JsonArray transformers;
	private JsonArray createList;
	private JsonArray dropList;
	private String snapshot=null;
	private String snapCondition=null;
	public Transformer(JsonObject jo) {
		transformer=jo;		
		data=transformer.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		evaluateCondition=data.getBoolean("evaluate",false);
		comment=data.getString("comment",null);
		if(!data.isNull("transformers"))
			transformers=data.getJsonArray("transformers");
		if(!data.isNull("createList"))
			createList=data.getJsonArray("createList");
		if(!data.isNull("dropList"))
			dropList=data.getJsonArray("dropList");
		snapshot=data.getString("snap",null);
		if(snapshot!=null && snapshot.equals("disabled"))
			snapshot=null;
		snapCondition=data.getString("snapCondition",null);
	}
	
    public void process(DataPipeline dp) throws SnippetException {
    	if(dp.isDestroyed())
			throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
    	if(disabled)
			return;
    	String snap=dp.getString("*snapshot");
		boolean canSnap = false;
		if(snap!=null || snapshot!=null) {
			canSnap = true;
			//snap=snapshot;
			if(snapshot!=null && snapshot.equals("conditional") && snapCondition!=null){
				canSnap =FlowUtils.evaluateCondition(snapCondition,dp);
				if(canSnap)
					dp.put("*snapshot","enabled");
			}else
				dp.put("*snapshot","enabled");
		}
		if(!canSnap)
			dp.drop("*snapshot");
		if(canSnap && snap==null) {
			dp.snap(comment);
		}
    	if(transformers!=null)
			FlowUtils.map(transformers, dp);
    	if(createList!=null)
			FlowUtils.setValue(createList, dp);
    	if(dropList!=null)
			FlowUtils.dropValue(dropList, dp);
    	if(canSnap) {
			dp.snap(comment);
			dp.drop("*snapshot");
		}else if(snap!=null)
			dp.put("*snapshot",snap);
	}
	
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isEvaluateCondition() {
		return evaluateCondition;
	}

	public void setEvaluateCondition(boolean evaluateCondition) {
		this.evaluateCondition = evaluateCondition;
	}

	public List<JsonOp> getOps() {
		return ops;
	}

	public void setOps(List<JsonOp> ops) {
		this.ops = ops;
	}

	
}
