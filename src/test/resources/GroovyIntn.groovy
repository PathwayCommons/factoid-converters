class GroovyIntn {
    void addIntn(java.util.Map props) {
    		com.google.gson.JsonObject template = props.get("template")
    		factoid.model.CustomizableModel model = props.get("model")
    		factoid.util.GsonUtil gsonUtil = new factoid.util.GsonUtil()
    		
    		com.google.gson.JsonArray ppts = template.get("participants").getAsJsonArray()
    		java.util.List<factoid.model.EntityModel> pptModels = gsonUtil.enityModelsFromJsonArray(ppts)
        print "Hello Groovy World\t" + pptModels[0].getName() + "\t" + pptModels[1].getName()
    }
}