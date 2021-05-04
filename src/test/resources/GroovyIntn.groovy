class GroovyIntn {
    void addIntn(java.util.Map props) {
    		com.google.gson.JsonObject template = props.get("template")
    		factoid.model.CustomizableModel model = props.get("model")
        print "Hello Groovy World" + model.toString() + "\t" + template.toString()
    }
}