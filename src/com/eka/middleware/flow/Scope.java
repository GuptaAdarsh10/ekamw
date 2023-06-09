package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Scope {
	private JsonObject data=null;
	private String snapshot=null;
	private String snapCondition=null;
	public Scope(JsonObject jo) {
		scope=jo;		
		data=scope.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		evaluateCondition=data.getBoolean("evaluate",false);
		comment=data.getString("comment",null);
		snapshot=data.getString("snap",null);
		if(snapshot!=null && snapshot.equals("disabled"))
			snapshot=null;
		snapCondition=data.getString("snapCondition",null);
	}
	
	public void process(DataPipeline dp) throws SnippetException{
		if(dp.isDestroyed())
			throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
		if(disabled)
			return;
		String snap=dp.getString("*snapshot");
		boolean canSnap = false;
		if(snap!=null || snapshot!=null) {
			canSnap = true;
			//snap=snapshot;
			if(snapshot!=null && snapshot.equals("conditional") && snapCondition!=null) {
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
		JsonArray flows= scope.getJsonArray("children");
		for (JsonValue jsonValue : flows) {
			String type=jsonValue.asJsonObject().getString("type",null);
			//System.out.println(type);
			switch(type) {
				case "try-catch":
					TCFBlock tcfBlock=new TCFBlock(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						tcfBlock.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(tcfBlock.getCondition(),dp);
						if(canExecute)
							tcfBlock.process(dp);
					}
				break;
				case "sequence":
					Scope scope=new Scope(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						scope.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(scope.getCondition(),dp);
						if(canExecute)
							scope.process(dp);
					}
				break;
				case "switch":
					Switch swich=new Switch(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						swich.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(swich.getCondition(),dp);
						if(canExecute)
							swich.process(dp);
					}
				break;
				case "loop":
					Loop loop=new Loop(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						loop.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(loop.getCondition(),dp);
						if(canExecute)
							loop.process(dp);
					}
				break;
				case "repeat":
					Repeat repeat=new Repeat(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						repeat.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(repeat.getCondition(),dp);
						if(canExecute)
							repeat.process(dp);
					}
				break;
				case "invoke":
					Invoke invoke=new Invoke(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						invoke.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(invoke.getCondition(),dp);
						if(canExecute)
							invoke.process(dp);
					}
				break;
				case "map":
					Transformer transformer=new Transformer(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						transformer.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(transformer.getCondition(),dp);
						if(canExecute)
							transformer.process(dp);
					}
				break;		
			}
		}
		if(canSnap) {
			dp.snap(comment);
			dp.drop("*snapshot");
		}else if(snap!=null)
			dp.put("*snapshot",snap);
	}
	
	public List<Scope> getScopes() {
		return scopes;
	}
	public void setScopes(List<Scope> scopes) {
		this.scopes = scopes;
	}
	public List<TCFBlock> getTcfBlocks() {
		return tcfBlocks;
	}
	public void setTcfBlocks(List<TCFBlock> tcfBlocks) {
		this.tcfBlocks = tcfBlocks;
	}
	public List<Invoke> getInvokes() {
		return invokes;
	}
	public void setInvokes(List<Invoke> invokes) {
		this.invokes = invokes;
	}
	public List<Repeat> getRepeats() {
		return repeats;
	}
	public void setRepeats(List<Repeat> repeats) {
		this.repeats = repeats;
	}
	public List<Loop> getLoops() {
		return loops;
	}
	public void setLoops(List<Loop> loops) {
		this.loops = loops;
	}
	public List<Transformer> getTransformers() {
		return transformers;
	}
	public void setTransformers(List<Transformer> transformers) {
		this.transformers = transformers;
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
	private List<Scope> scopes;
	private List<TCFBlock> tcfBlocks;
	private List<Invoke> invokes;
	private List<Repeat> repeats;
	private List<Loop> loops;
	private List<Transformer> transformers;	
	private boolean disabled=false;
	private String condition;
	private String label;
	private boolean evaluateCondition;
	private JsonObject scope;
	private String comment;
}
