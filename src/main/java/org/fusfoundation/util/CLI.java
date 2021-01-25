/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.fusfoundation.kranion.Main;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.fusfoundation.kranion.model.image.ImageVolume;

import java.net.*;
import java.io.*; 

/**
 *
 * @author mkomaiha
 */

public class CLI extends Thread {
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private Main main = null;
 	
    public CLI(Main main) {
    	this.main = main;
        // establish a connection 
    	try {
            this.serverSocket = new ServerSocket(9000);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    } 
    
    @Override
    public void run() {
    	try {
            while(true) {
                try {
                    clientSocket = serverSocket.accept();
                    JsonObject jsonObject = JsonParser.parseReader​(new InputStreamReader(clientSocket.getInputStream())).getAsJsonObject();
//                     Send a JSON array with multiple update types that we loop over?
//                    [
//                      {
//                        "msgType": "REGISTRATION",
//                        "attribute": "quaternion",
//                        "data": [x,y,z,w]
//                      },
//                      {
//                        "msgType": "REGISTRATION",
//                        "attribute": "translation",
//                        "data": [x,y,z]
//                      }, ...{}
//                    ]
                    if (jsonObject.has("msgType")) {
                        JsonElement msgType = jsonObject.get("msgType");
                        switch (msgType.getAsString()) {
                            case "REGISTRATION":
                                System.out.println("Registration");
                                if (jsonObject.has("attribute")) {
                                    JsonElement attrib = jsonObject.get("attribute");
                                    switch (attrib.getAsString()) {
                                        case "quaternion":
                                            if (jsonObject.has("data")) {
                                                JsonElement data = jsonObject.get("data");
                                                if (data.isJsonArray()) {
                                                    JsonArray dataArr = data.getAsJsonArray();
                                                    if (dataArr.size() == 4) {
                                                        System.out.println(data);
                                                        Quaternion obj = new Quaternion(dataArr.get(0).getAsFloat(),
                                                                dataArr.get(1).getAsFloat(),
                                                                dataArr.get(2).getAsFloat(),
                                                                dataArr.get(3).getAsFloat());
                                                        ImageVolume img = main.getModel().getCtImage();
                                                        if (img != null) {
                                                            img.setAttribute("ImageOrientationQ", obj);
                                                            main.getView().setIsDirty(true);
                                                        } else {
                                                            System.out.print("Import CT Image First");
                                                        }
                                                    } else {
                                                        System.out.println("data array must be of length 4");
                                                    }
                                                } else {
                                                    System.out.println("data must be a JSONArray");
                                                }
                                            }
                                            break;
                                        case "translation":
                                            if (jsonObject.has("data")) {
                                                JsonElement data = jsonObject.get("data");
                                                if (data.isJsonArray()) {
                                                    JsonArray dataArr = data.getAsJsonArray();
                                                    if (dataArr.size() == 3) {
                                                        System.out.println(data);
                                                        Vector3f translation = new Vector3f(dataArr.get(0).getAsFloat(),
                                                                dataArr.get(1).getAsFloat(),
                                                                dataArr.get(2).getAsFloat());
                                                        ImageVolume img = main.getModel().getCtImage();
                                                        if (img != null) {
                                                            img.setAttribute("ImageTranslation", translation);
                                                            main.getView().setIsDirty(true);
                                                        } else {
                                                            System.out.print("Import CT Image First");
                                                        }
                                                    } else {
                                                        System.out.println("data array must be of length 3");
                                                    }
                                                } else {
                                                    System.out.println("data must be a JSONArray");
                                                }
                                            }
                                            break;
                                        default:
                                            System.out.print("Unknown attribute type");
                                            break;
                                    }
                                }
                                break;
                            default:
                                System.out.print("Unknown msg type");
                                break;
                        }
                    }
                } catch (JsonParseException e) {
                    System.out.print("Invalid or malformed JSON string");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    	} finally {
            try {
                clientSocket.close();
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    	}
    }
}
