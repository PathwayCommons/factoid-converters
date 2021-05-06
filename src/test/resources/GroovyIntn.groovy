class GroovyIntn {
    void addIntn(java.util.Map props) {
    		com.google.gson.JsonObject template = props.get("template")
    		factoid.model.CustomizableModel model = props.get("model")
    		factoid.util.GsonUtil gsonUtil = new factoid.util.GsonUtil()
    		
    		com.google.gson.JsonArray ppts = template.get("participants").getAsJsonArray()
    		com.google.gson.JsonObject ppt = ppts[0]
    		
    		java.lang.String name = ppt.get("name")
    		java.util.List<factoid.model.EntityModel> componentModels = gsonUtil.enityModelsFromJsonArray(ppt.get("components").getAsJsonArray())
    		
    		model.getOrCreatePhysicalEntity(org.biopax.paxtools.model.level3.Complex.class, name, null, false, componentModels)
    }
}