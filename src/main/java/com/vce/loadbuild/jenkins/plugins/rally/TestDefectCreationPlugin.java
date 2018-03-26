package com.vce.loadbuild.jenkins.plugins.rally;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.Request;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.rallydev.rest.util.Ref;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 
 * @author rousef
 * This is used as a scratch pad for different tests that utilize the RallyUtils without
 * having to go through the compile/package/deploy phase of Jenkins just to test.
 *
 */
public class TestDefectCreationPlugin {

    private final static String RALLY_API_KEY = "_rwzUEl3sTruRZoHisJOnfkwhGsAeEhPgILFrbR0aKQ";
    private final static String VCE_WORKSPACE = "";

    
    private static boolean givenDefectValueAllowable(String field, String value) throws URISyntaxException, IOException {
        // Set a boolean so we have a single point of exit.
        boolean found = false;
        // Pad with quotation marks as this is what is returned from the query.
        value = "\"" + value + "\"";

        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RALLY_API_KEY);
        QueryRequest typeDefRequest = new QueryRequest("TypeDefinition");
        typeDefRequest.setFetch(new Fetch("ObjectID", "Attributes"));
        typeDefRequest.setQueryFilter(new QueryFilter("Name", "=", "Defect"));
        QueryResponse typeDefQueryResponse = restApi.query(typeDefRequest);
        JsonObject typeDefJsonObject = typeDefQueryResponse.getResults().get(0).getAsJsonObject();
        QueryRequest attributeRequest = new QueryRequest(typeDefJsonObject.getAsJsonObject("Attributes"));
        attributeRequest.setFetch(new Fetch("AllowedValues", "ElementName", "Name"));
        QueryResponse attributeQueryResponse = restApi.query(attributeRequest);
        for (int i=0; i < attributeQueryResponse.getResults().size();i++) {
            String fieldName = attributeQueryResponse.getResults().get(i).getAsJsonObject().get("Name").getAsString();
            if (fieldName.equals(field)) {
                JsonObject allowedValuesJsonObject = attributeQueryResponse.getResults().get(i).getAsJsonObject();
                int numberOfAllowedValues = allowedValuesJsonObject.getAsJsonObject("AllowedValues").get("Count").getAsInt();
                QueryRequest allowedValuesRequest = new QueryRequest(allowedValuesJsonObject.getAsJsonObject("AllowedValues"));
                allowedValuesRequest.setFetch(new Fetch("StringValue"));
                QueryResponse allowedValuesResponse = restApi.query(allowedValuesRequest);
                for (int j=0; j < numberOfAllowedValues; j++) {
                    JsonObject allowedAttributeValuesJsonObject = allowedValuesResponse.getResults().get(j).getAsJsonObject();
                    if (value.equals(allowedAttributeValuesJsonObject.get("StringValue").toString())) {
                        found = true;
                        break;
                    }
                }
                // To short change the outside for loop once we have found the value.
                break;
            }
        }
        restApi.close();
        return found;
    }


    private static void listAllRallyProjects() throws URISyntaxException, IOException {
        RallyRestApi restApi = new RallyRestApi(new URI("https://rally1.rallydev.com"), RALLY_API_KEY);
        QueryRequest projectRequest = new QueryRequest("Project");
        projectRequest.setFetch(new Fetch ("Name"));
        projectRequest.setWorkspace(VCE_WORKSPACE);
        QueryResponse projectQueryResponse = restApi.query(projectRequest);
        if (projectQueryResponse.wasSuccessful()) {
            List<String> projectList = new ArrayList<String>();

            System.out.println("Number of projects = " + projectQueryResponse.getResults().size());
            JsonArray projects = projectQueryResponse.getResults();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            for (JsonElement tempJson : projects) {
                String json = gson.toJson(tempJson.getAsJsonObject());
                System.out.println("\n" + json+ "\n");
                projectList.add(tempJson.getAsJsonObject().get("Name").toString());
            }
            

            Collections.sort(projectList);
            int max_line_length = 120;
            int current_line_length = 0;
            StringBuilder output_line = new StringBuilder();
            for (String tempString : projectList) {
                if (tempString.length() + current_line_length > max_line_length) {
                    output_line.append("\n");
                    current_line_length = 0;
                }
                output_line.append(tempString + ", ");
                current_line_length = current_line_length + tempString.length() + 2;
            }
            output_line.deleteCharAt(output_line.lastIndexOf(", "));
            System.out.println(output_line);
        } else {
            System.out.println("Unsuccessful query.");
            String errors[] = projectQueryResponse.getErrors();
            for (int i = 0; i < errors.length; i++) {
                System.out.println(errors[i]);
            }
        }
        restApi.close();
    }


    private static void listAllRallyWorkspaces() throws URISyntaxException, IOException {
        RallyRestApi restApi = new RallyRestApi(new URI("https://rally1.rallydev.com"), RALLY_API_KEY);
        QueryRequest projectRequest = new QueryRequest("Workspaces");
        projectRequest.setFetch(new Fetch ("Name", "_ref"));
        QueryResponse workspaceQueryResponse = restApi.query(projectRequest);
        if (workspaceQueryResponse.wasSuccessful()) {
            List<String> workspaceList = new ArrayList<String>();

            System.out.println("Number of workspaces = " + workspaceQueryResponse.getResults().size());
            JsonArray workspaces = workspaceQueryResponse.getResults();
            for (JsonElement tempJson : workspaces) {
                workspaceList.add(tempJson.getAsJsonObject().get("Name").toString());
                System.out.println("Ref = " + tempJson.getAsJsonObject().get("_ref").toString());
            }

            Collections.sort(workspaceList);
            int max_line_length = 120;
            int current_line_length = 0;
            StringBuilder output_line = new StringBuilder();
            for (String tempString : workspaceList) {
                if (tempString.length() + current_line_length > max_line_length) {
                    output_line.append("\n");
                    current_line_length = 0;
                }
                output_line.append(tempString + ", ");
                current_line_length = current_line_length + tempString.length() + 2;
            }
            output_line.deleteCharAt(output_line.lastIndexOf(", "));
            System.out.println(output_line);
        } else {
            System.out.println("Unsuccessful query.");
            String errors[] = workspaceQueryResponse.getErrors();
            for (int i = 0; i < errors.length; i++) {
                System.out.println(errors[i]);
            }
        }
        restApi.close();
    }
    
    
    private static String getWorkspaceReference(String workspaceName) throws URISyntaxException, IOException {
        String workspaceReference = "";
        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RALLY_API_KEY);
        QueryRequest projectRequest = new QueryRequest("Workspaces");
        projectRequest.setQueryFilter(new QueryFilter("Name", "=", workspaceName));
        projectRequest.setFetch(new Fetch ("_ref"));
        QueryResponse workspaceQueryResponse = restApi.query(projectRequest);
        if (workspaceQueryResponse.wasSuccessful()) {
            if (workspaceQueryResponse.getResults().size() >= 1) {
                String full_reference = workspaceQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").toString();
                System.out.println("full_reference      = " + full_reference);
                workspaceReference = full_reference.substring(full_reference.indexOf("workspace") - 1);
                System.out.println("workspace_reference = " + workspaceReference);
            } else {
                System.out.println("Unable to find a reference for workspace " + workspaceName);
            }
        } else {
            System.out.println("Unsuccessful workspace query.");
            String errors[] = workspaceQueryResponse.getErrors();
            for (int i = 0; i < errors.length; i++) {
                System.out.println(errors[i]);
            }
        }
        restApi.close();
        return workspaceReference;
    }

    
    
    

    
    
    
    
    

    public static void main(String[] args) throws URISyntaxException, IOException {

        //Create and configure a new instance of RallyRestApi
        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RALLY_API_KEY);
        restApi.setApplicationName("TestDefectCreation");
        String user_ref = null;

        try {
//            listAllRallyProjects();
//            listAllRallyWorkspaces();
//              String test_string = getWorkspaceReference("VCE");
            String workspaceRef = RallyUtils.getWorkspaceReference("VCE", RALLY_API_KEY);
//            ArrayList<String> foundInVersionValues = RallyUtils.allowedValues("Defect", "Found in Version", workspaceRef, RALLY_API_KEY);
//            Collections.sort(foundInVersionValues);
//            for (String tempString: foundInVersionValues)
//                System.out.println("Value " + tempString);
//            listAllRallyProjects();
//              String test_string = RallyUtils.getWorkspaceReference("VCE", RALLY_API_KEY);
//              System.out.println("test_string = " + test_string);
//              test_string = RallyUtils.getWorkspaceReference("VEE", RALLY_API_KEY);
//              System.out.println("test_string = " + test_string);
              
//            QueryRequest userInfo = new QueryRequest("user");
//            userInfo.setQueryFilter(new QueryFilter("UserName", "=", "pebuildrelease@vce.com"));
//            QueryResponse queryresponse = restApi.query(userInfo);
//            if (queryresponse.wasSuccessful()) {
//                JsonArray results = queryresponse.getResults();
//                JsonObject user = results.get(0).getAsJsonObject();
//                user_ref = user.get("_ref").getAsString();
//                System.out.println("user_ref = " + user_ref);
////                user = results.getAsJsonObject();
//            }
            String newWorkspaceRef = RallyUtils.getWorkspaceReference("VCE", RALLY_API_KEY);
            System.out.println("newWorkspaceRef = " + newWorkspaceRef);
            System.out.println("RALLY_API_KEY   = " + RALLY_API_KEY);
            ArrayList<String> allowedValues = RallyUtils.allowedFieldValues("Defect", "Found in Version", newWorkspaceRef, RALLY_API_KEY);
            Integer size = new Integer(allowedValues.size());
            System.out.println ("allowedValues size = " + size.toString());
            for (String tempString: allowedValues)
                System.out.println("Value = " + tempString);
            System.exit(0);
            // Subset of code to get user
            QueryRequest typeDefRequest = new QueryRequest("TypeDefinition");
            typeDefRequest.setFetch(new Fetch("ObjectID", "Attributes"));
            typeDefRequest.setQueryFilter(new QueryFilter("Name", "=", "Defect"));
            QueryResponse typeDefQueryResponse = restApi.query(typeDefRequest);
            JsonObject typeDefJsonObject = typeDefQueryResponse.getResults().get(0).getAsJsonObject();
            QueryRequest attributeRequest = new QueryRequest(typeDefJsonObject.getAsJsonObject("Attributes"));
            attributeRequest.setFetch(new Fetch("AllowedValues", "ElementName", "Name"));
            QueryResponse attributeQueryResponse = restApi.query(attributeRequest);
            for (int i=0; i<attributeQueryResponse.getResults().size();i++) {
                String fieldName = attributeQueryResponse.getResults().get(i).getAsJsonObject().get("Name").getAsString();
                System.out.println("fieldName = " + fieldName);
                if (fieldName.equals("Found in Version")) {
                    JsonObject allowedValuesJsonObject = attributeQueryResponse.getResults().get(i).getAsJsonObject();
                    int numberOfAllowedValues = allowedValuesJsonObject.getAsJsonObject("AllowedValues").get("Count").getAsInt();
                    QueryRequest allowedValuesRequest = new QueryRequest(allowedValuesJsonObject.getAsJsonObject("AllowedValues"));
                    allowedValuesRequest.setFetch(new Fetch("StringValue"));
                    QueryResponse allowedValuesResponse = restApi.query(allowedValuesRequest);
                    for (int j=0; j < numberOfAllowedValues; j++) {
                        JsonObject allowedAttributeValuesJsonObject = allowedValuesResponse.getResults().get(j).getAsJsonObject();
                        System.out.println(allowedAttributeValuesJsonObject.get("StringValue"));
                    }
                }
            }
            restApi.close();
            System.exit(0);
            
//            restApi.query(arg0)
            // BRM_Build_Failure

            //Create a defect
            System.out.println("Creating defect...");
            JsonObject newDefect = new JsonObject();
            newDefect.addProperty("Name",                             "Jenkins Plugin Test Defect");
            newDefect.addProperty("Severity",                         "Major Problem");
            newDefect.addProperty("Priority",                         "Resolve Immediately");
            newDefect.addProperty("SubmittedBy",                      user_ref);
            newDefect.addProperty("c_DefectType",                     "Defect");
            newDefect.addProperty("c_Methodtoidentifysimilardefects", "N/A");
            newDefect.addProperty("c_WhereFound",                     "N/A");
            newDefect.addProperty("c_DefectCategory",                 "N/A");
            newDefect.addProperty("c_FoundinVersion",                 "N/A");
            newDefect.addProperty("c_WhereIntroduced",                "N/A");
            CreateRequest createRequest = new CreateRequest("defect", newDefect);

            CreateResponse createResponse = restApi.create(createRequest);
            System.out.println(String.format("Created %s", createResponse.getObject().get("_ref").getAsString()));

            //Read defect
            String ref = Ref.getRelativeRef(createResponse.getObject().get("_ref").getAsString());
            System.out.println(String.format("\nReading defect %s...", ref));
            GetRequest getRequest = new GetRequest(ref);
            GetResponse getResponse = restApi.get(getRequest);
            JsonObject obj = getResponse.getObject();
            System.out.println(String.format("Read defect. Name = %s, State = %s",
                    obj.get("Name").getAsString(), obj.get("State").getAsString()));

            //Update defect
            System.out.println("\nUpdating defect state...");
            JsonObject updatedDefect = new JsonObject();
            updatedDefect.addProperty("State", "Fixed");
            UpdateRequest updateRequest = new UpdateRequest(ref, updatedDefect);
            UpdateResponse updateResponse = restApi.update(updateRequest);
            obj = updateResponse.getObject();
            System.out.println(String.format("Updated defect. State = %s", obj.get("State").getAsString()));

            //Delete defect
            System.out.println("\nDeleting defect...");
            DeleteRequest deleteRequest = new DeleteRequest(ref);
            DeleteResponse deleteResponse = restApi.delete(deleteRequest);
            if (deleteResponse.wasSuccessful()) {
                System.out.println("Deleted defect.");
            }

        } finally {
            //Release all resources
            restApi.close();
        }
    }
}
