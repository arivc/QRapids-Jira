package com.qrapids.backlog_jira.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qrapids.backlog_jira.data.ErrorResponse;
import com.qrapids.backlog_jira.data.Milestone;
import com.qrapids.backlog_jira.data.QualityRequirement;
import com.qrapids.backlog_jira.data.SuccessResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class BacklogService {
    @GetMapping("/api/milestones")
    public ResponseEntity<Object> gletkkMilestones(@RequestParam String project_id,
                                                @RequestParam(value = "date_from", required = false) String date_from) {
        try {

            //Creating the request authentication by username and apiKey
            ClientResponse con;
            String auth = new String(Base64.encode("ariadna.vinets@estudiantat.upc.edu" + ":" + "BaDD9uM0B6VtdOWuwRkQ3C2B"));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";

            Client client = Client.create();

            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/search?jql=project%3D" + project_id + "%20AND%20issuetype%3Dmilestone");
            con = webResource.header(headerAuthorization , headerAuthorizationValue).type(headerType).accept(headerType).get(ClientResponse.class);
            int status = con.getStatus();
            System.out.println(status);
            // Creating a Request with authentication by token



            // Reading the Response
            if (status != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + status);
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getEntityInputStream(), "utf-8"));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                System.out.println(content.toString());
                in.close();
                con.close();

                JsonParser parser = new JsonParser();
                JsonArray data = parser.parse(content.toString()).getAsJsonArray();
                JsonObject obj = (JsonObject) data.getAsJsonObject().get("issues");
                System.out.println(obj.toString());
                List<Milestone> milestones = new ArrayList<>();
                for (int i = 0; i < data.size(); ++i) {
                    JsonObject object = data.get(i).getAsJsonObject();
                    if (!object.get("due_date").isJsonNull()) { // check if milestone have due_date
                        String date = object.get("due_date").getAsString();
                        if(date_from != null && !date_from.isEmpty()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Date from = sdf.parse(date_from);
                            Date due = sdf.parse(date);
                            if (due.equals(from) || due.after(from)) { // only add milestones which will finish after date_from
                                Milestone newMilestone = new Milestone();
                                newMilestone.setName(object.get("title").getAsString());
                                newMilestone.setDate(date);
                                newMilestone.setDescription(object.get("description").getAsString());
                                newMilestone.setType("Milestone");
                                milestones.add(newMilestone);
                            }
                        } else { // if there is no date_from specified --> we add all milestones with due_date
                            Milestone newMilestone = new Milestone();
                            newMilestone.setName(object.get("title").getAsString());
                            newMilestone.setDate(date);
                            newMilestone.setDescription(object.get("description").getAsString());
                            newMilestone.setType("Milestone");
                            milestones.add(newMilestone);
                        }
                    }
                }
                Collections.sort(milestones, (Milestone o1, Milestone o2) -> o1.getDate().compareTo(o2.getDate()));
                return new ResponseEntity<>(milestones, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/api/createIssue")
    public ResponseEntity<Object> createIssue(@RequestBody QualityRequirement requirement) throws IOException {
        try {
            SuccessResponse newIssue = null;
            //Creating the request authentication by username and apiKey
            ClientResponse response;
            String auth = new String(Base64.encode("ariadna.vinets@estudiantat.upc.edu" + ":" + "BaDD9uM0B6VtdOWuwRkQ3C2B"));
            final String headerAuthorization = "Authorization";
            final String headerAuthorizationValue = "Basic " + auth;
            final String headerType = "application/json";

            Client client = Client.create();

            String data1 = "{\"fields\":{\"project\":{\"key\":\"TP\"},\"summary\":\"REST Test\",\"description\": \"Creating of an issue using project keys and issue type names using the REST API\",\"issuetype\":{\"name\":\"Bug\"}}}";

            //Creating all the data with the requirement
            String data = "{\"fields\":{\"project\":{\"key\":\"" + requirement.getProject_id() + "\""
                    + "},\"summary\":\"" + requirement.getIssue_summary() + "\""
                    + ",\"description\":\" " + requirement.getIssue_description() + "\"" + ",\"issuetype\":{\"name\":\""
                    + requirement.getIssue_type() + "\"" + "}}}";

            //POST METHOD JIRA
            WebResource webResource = client.resource("https://ariadnavinets.atlassian.net/rest/api/2/issue");
            response = webResource.header(headerAuthorization, headerAuthorizationValue).type(headerType).accept(headerType).post(ClientResponse.class, data);
            int statusCode = response.getStatus();
            System.out.println(statusCode);


            if (statusCode != 201) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + statusCode);

            } else {
                try
                        (BufferedReader br = new BufferedReader(
                                new InputStreamReader(response.getEntityInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder resp = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        resp.append(responseLine.trim());
                    }
                    System.out.println(resp.toString());

                    JsonParser parser = new JsonParser();
                    JsonObject object = parser.parse(resp.toString()).getAsJsonObject();

                    System.out.println(object);
                    newIssue = new SuccessResponse(object.get("id").getAsString(), object.get("self").getAsString());
                    return new ResponseEntity<>(newIssue, HttpStatus.OK);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}


