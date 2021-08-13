class GroovyIntn {
    void addIntn(java.util.Map props) {
    		com.google.gson.JsonObject template = props.get("template")
    		factoid.model.CustomizableModel model = props.get("model")
    		factoid.util.GsonUtil gsonUtil = new factoid.util.GsonUtil()
    		
    		com.google.gson.JsonArray ppts = template.get("participants").getAsJsonArray()
    		com.google.gson.JsonObject ppt = ppts[0]
    		
    		factoid.model.EntityModel entityModel =  gsonUtil.entityModelFromJsonObj(ppt)
    		
    		model.physicalEntityFromModel(entityModel)
    }
}