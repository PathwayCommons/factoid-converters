class GroovyIntn {
    void addIntn(java.util.Map props) {
    		com.google.gson.JsonObject template = props.get("template")
    		factoid.model.CustomizableModel model = props.get("model")
    		factoid.util.GsonUtil gsonUtil = new factoid.util.GsonUtil()
    		
    		com.google.gson.JsonArray entityObjs = template.get("participants").getAsJsonArray()
    		com.google.gson.JsonObject leftEntityObj = entityObjs[0]
    		com.google.gson.JsonObject rightEntityObj = entityObjs[1]
    		factoid.model.EntityModel leftEntityModel =  gsonUtil.entityModelFromJsonObj(leftEntityObj)
    		factoid.model.EntityModel rightEntityModel =  gsonUtil.entityModelFromJsonObj(rightEntityObj)
		
		org.biopax.paxtools.model.level3.Protein left = model.physicalEntityFromModel(leftEntityModel)
		org.biopax.paxtools.model.level3.Protein right = model.physicalEntityFromModel(rightEntityModel)
		
		org.biopax.paxtools.model.level3.Transport transport = model.addNewInteraction(org.biopax.paxtools.model.level3.Transport.class)
		transport.addLeft(left)
		transport.addRight(right)
		
		transport.addComment("http://www.w3.org/2001/XMLSchema#string\">REPLACED http://pathwaycommons.org/pc12/Transport_e5c5c3064a35275da036764754d82987")
    		
    }
}