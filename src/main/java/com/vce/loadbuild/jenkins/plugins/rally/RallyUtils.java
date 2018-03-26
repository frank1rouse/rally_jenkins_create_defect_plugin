package com.vce.loadbuild.jenkins.plugins.rally;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;


public class RallyUtils {
    public static final String RALLY_URL = "https://rally1.rallydev.com";

    // Store previous queries to speed up the process
    private static Map<String, String> workspaceRef = new HashMap<String, String>();

    public static String getWorkspaceReference(String workspaceName, String RallyApiKey) throws URISyntaxException, IOException {

        // Check to see if we have used this value before.
        String workspaceReference = workspaceRef.get(workspaceName);

        // I guess we haven't asked about this one before
        if (workspaceReference == null) {
            RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RallyApiKey);
            QueryRequest projectRequest = new QueryRequest("Workspaces");
            projectRequest.setQueryFilter(new QueryFilter("Name", "=", workspaceName));
            projectRequest.setFetch(new Fetch("_ref"));
            QueryResponse workspaceQueryResponse = restApi.query(projectRequest);
            if (workspaceQueryResponse.wasSuccessful()) {
                if (workspaceQueryResponse.getResults().size() > 0) {
                    String full_reference = workspaceQueryResponse.getResults().get(0).getAsJsonObject().get("_ref").toString();
                    workspaceReference = full_reference.substring(full_reference.indexOf("workspace") - 1).replace("\"", "");
                    workspaceRef.put(workspaceName, workspaceReference);
                } else {
                    System.out.println("Unable to find a reference for workspace \"" + workspaceName + "\"");
                }
            } else {
                System.out.println("Unsuccessful workspace query.");
                String errors[] = workspaceQueryResponse.getErrors();
                for (String tempString : errors) {
                    System.out.println(tempString);
                }
            }
            restApi.close();
        }
        return workspaceReference;
    }

/**
 * 
 * @param workspaceRef - limits the search to a single workspace.
 * @param field - the field that we are looking for values.
 * @param value - the value that we test is part of the list of allowable values.
 * @param RallyAPIKey
 * @return
 * @throws URISyntaxException
 * @throws IOException
 * Developed earlier in the plugin process. Not used anymore but kept as a reference.
 */
    public static boolean givenDefectValueAllowable(String workspaceRef, String field, String value, String RallyAPIKey) throws URISyntaxException, IOException {
        // Set a boolean so we have a single point of exit.
        boolean found = false;
        // Pad with quotation marks as this is what is returned from the query.
        value = "\"" + value + "\"";

        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RallyAPIKey);
        QueryRequest typeDefRequest = new QueryRequest("TypeDefinition");
        typeDefRequest.setWorkspace(workspaceRef);
        typeDefRequest.setFetch(new Fetch("ObjectID", "Attributes"));
        typeDefRequest.setQueryFilter(new QueryFilter("Name", "=", "Defect"));
        QueryResponse typeDefQueryResponse = restApi.query(typeDefRequest);
        JsonObject typeDefJsonObject = typeDefQueryResponse.getResults().get(0).getAsJsonObject();
        QueryRequest attributeRequest = new QueryRequest(typeDefJsonObject.getAsJsonObject("Attributes"));
        attributeRequest.setFetch(new Fetch("AllowedValues", "ElementName", "Name"));
        QueryResponse attributeQueryResponse = restApi.query(attributeRequest);
        if (attributeQueryResponse.wasSuccessful()) {
            for (JsonElement tempFieldQueryResponse: attributeQueryResponse.getResults()) {
                String fieldName = tempFieldQueryResponse.getAsJsonObject().get("Name").getAsString();
                if (fieldName.equals(field)) {
                    JsonObject allowedValuesJsonObject = tempFieldQueryResponse.getAsJsonObject();
                    QueryRequest allowedValuesRequest = new QueryRequest(allowedValuesJsonObject.getAsJsonObject("AllowedValues"));
                    allowedValuesRequest.setFetch(new Fetch("StringValue"));
                    QueryResponse allowedValuesResponse = restApi.query(allowedValuesRequest);
                    if (allowedValuesResponse.wasSuccessful()) {
                        for (JsonElement tempAllowedValuesReponse: allowedValuesResponse.getResults()) {
                            JsonObject allowedAttributeValuesJsonObject = tempAllowedValuesReponse.getAsJsonObject();
                            if (value.equals(allowedAttributeValuesJsonObject.get("StringValue").toString())) {
                                found = true;
                                // Early break out of the loop if we find the value
                                break;
                            }
                        }
                    } else {
                        restApi.close();
                        throw new IOException("Unable to retrieve values for field \"" + field + "\".");
                    }
                    // To short change the outside for loop once we have found the field.
                    break;
                }
            }
        } else {
            restApi.close();
            throw new IOException("Unable to find the field \"" + field + "\".");
        }
        restApi.close();
        return found;
    }

/**
 * 
 * @param workspaceRef Rally workspace ref
 * @param projectName
 * @param RallyAPIKey
 * @return Rally project reference if found, null if nothing found.
 * @throws URISyntaxException
 * @throws IOException
 */
    public static String getProjectReference(String workspaceRef, String projectName, String RallyAPIKey) throws URISyntaxException, IOException {
        String projectRef = null;
        RallyRestApi restApi = new RallyRestApi(new URI(RALLY_URL), RallyAPIKey);
        QueryRequest projectRequest = new QueryRequest("Project");
        projectRequest.setFetch(new Fetch ("Name", "_ref"));
        projectRequest.setWorkspace(workspaceRef);
        QueryResponse projectQueryResponse = restApi.query(projectRequest);
        // If the projectQueryResponse was not successful then just return a null referece. Needs to be checked on the calling platform.
        if (projectQueryResponse.wasSuccessful())
            for (JsonElement tempJson : projectQueryResponse.getResults()) {
                if (projectName.equalsIgnoreCase(tempJson.getAsJsonObject().get("Name").getAsString())) {
                    String fullProjectReferece = tempJson.getAsJsonObject().get("_ref").getAsString();
                    // Grab
                    projectRef = fullProjectReferece.substring(fullProjectReferece.indexOf("project") - 1).replace("\"", "");
                    // Short change the loop once we have found our project
                    break;
                }
            }
        restApi.close();
        return projectRef;
    }


    /**
     * 
     * @param object - Example "Defect"
     * @param field - Example "Found In Version"
     * @param workspaceRef
     * @param RallyAPIKey
     * @return ArrayList<String> of allowable values - names only
     * @throws URISyntaxException
     * @throws IOException - Overloaded to allow for failed queries.
     */
    public static ArrayList<String> allowedFieldValues(String object, String field, String workspaceRef, String RallyAPIKey) throws URISyntaxException, IOException {
        ArrayList<String> allowedValues = new ArrayList<String>();
        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RallyAPIKey);
        QueryRequest typeDefRequest = new QueryRequest("TypeDefinition");
        typeDefRequest.setWorkspace(workspaceRef);
        typeDefRequest.setFetch(new Fetch("ObjectID", "Attributes"));
        typeDefRequest.setQueryFilter(new QueryFilter("Name", "=", object));
        QueryResponse typeDefQueryResponse = restApi.query(typeDefRequest);
        if (typeDefQueryResponse.wasSuccessful()) {
            JsonObject typeDefJsonObject = typeDefQueryResponse.getResults().get(0).getAsJsonObject();
            QueryRequest attributeRequest = new QueryRequest(typeDefJsonObject.getAsJsonObject("Attributes"));
            attributeRequest.setFetch(new Fetch("AllowedValues", "ElementName", "Name"));
            QueryResponse attributeQueryResponse = restApi.query(attributeRequest);
            if (attributeQueryResponse.wasSuccessful()) {
                for (JsonElement tempAttributeQueryResponseElement : attributeQueryResponse.getResults()) {
                    if (field.equals(tempAttributeQueryResponseElement.getAsJsonObject().get("Name").getAsString())) {
                        JsonObject allowedValuesJsonObject = tempAttributeQueryResponseElement.getAsJsonObject();
                        QueryRequest allowedValuesRequest = new QueryRequest(allowedValuesJsonObject.getAsJsonObject("AllowedValues"));
                        allowedValuesRequest.setFetch(new Fetch("StringValue"));
                        QueryResponse allowedValuesResponse = restApi.query(allowedValuesRequest);
                        if (allowedValuesResponse.wasSuccessful()) {
                            for (JsonElement tempAllowedValuesElement : allowedValuesResponse.getResults())
                                // Add project name to list while stripping out quotation marks
                                allowedValues.add(tempAllowedValuesElement.getAsJsonObject().get("StringValue").toString().replace("\"", ""));
                            // To short change the outside for loop once we have found the value.
                            break;
                        } else {
                            // Ensure that we always close the restApi connection.
                            restApi.close();
                            throw new IOException("Failed AllowedValues query of the field \"" + field + "\".");
                        }
                    }
                }
            } else {
                // Ensure that we always close the restApi connection.
                restApi.close();
                throw new IOException("Failed Attributes query of the object \"" + object + " \".");
            }
        } else {
            // Ensure that we always close the restApi connection.
            restApi.close();
            throw new IOException("Failed TypeDefinition query of the \"" + object + "\".");
        }
        restApi.close();
        Collections.sort(allowedValues);
        return allowedValues;
    }


    public static ArrayList<String> listAllRallyProjects(String workspaceRef, String RallyAPIKey) throws URISyntaxException, IOException {
        ArrayList<String> projectList = new ArrayList<String>();
        RallyRestApi restApi = new RallyRestApi(new URI(RallyUtils.RALLY_URL), RallyAPIKey);
        QueryRequest projectRequest = new QueryRequest("Project");
        projectRequest.setFetch(new Fetch ("Name"));
        projectRequest.setWorkspace(workspaceRef);
        QueryResponse projectQueryResponse = restApi.query(projectRequest);
        if (projectQueryResponse.wasSuccessful()) {
            JsonArray projects = projectQueryResponse.getResults();
            for (JsonElement tempJson : projects)
                // Add project name to list while stripping out quotation marks
                projectList.add(tempJson.getAsJsonObject().get("Name").toString().replace("\"", ""));
            Collections.sort(projectList);
        } else {
            // Ensure that we always close the restApi connection.
            restApi.close();
            throw new IOException("Failed to query the proejcts for the workspace \"" + workspaceRef + "\".");
        }
        restApi.close();
        return projectList;
    }
}
