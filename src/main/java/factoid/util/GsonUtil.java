package factoid.util;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import factoid.model.EntityModel;

public class GsonUtil {
	private Gson gson;
	
	public GsonUtil() {
		gson = new Gson();
	}
	
	public EntityModel entityModelFromJsonObj(JsonObject obj) {
		EntityModel model = gson.fromJson(obj, EntityModel.class);
		return model;
	}
	
	public List<EntityModel> enityModelsFromJsonArray(JsonArray jsArr){
		List<EntityModel> models = gson.fromJson(jsArr, new TypeToken<List<EntityModel>>(){}.getType());
		return models;
	}
}
