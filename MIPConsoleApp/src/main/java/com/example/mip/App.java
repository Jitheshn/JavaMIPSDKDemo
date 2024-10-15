package com.example.mip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

import com.microsoft.informationprotection.ApplicationInfo;
import com.microsoft.informationprotection.AssignmentMethod;
import com.microsoft.informationprotection.DataState;

public class App {
	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        // ApplicationInfo is used to store the application name, clientId, and version.
        ApplicationInfo appInfo = new ApplicationInfo();
        FileOptions options = new FileOptions();

        appInfo.setApplicationId("62b54240-fcbd-4f46-aa3e-30dad67d4dba");
        appInfo.setApplicationName("MIP SDK Java Sample");
        appInfo.setApplicationVersion("1.15");
       

        System.out.print("Enter a username: ");
        String userName = reader.readLine();

        LabelManager lblMgr = new LabelManager(appInfo, userName);

        // Fetch the list of labels for the authenticated user and display.
        lblMgr.ListLabels();

        // Copy a label Id from the output and paste into the prompt.
        System.out.print("Enter a label Id: ");
        options.LabelId = reader.readLine();

        // Provide an input file that should be labeled.
        System.out.print("Enter an input file full path: ");
        options.InputFilePath = reader.readLine();

        // Provide the output path for the file. The original file remains intact and a
        // copy is created.
        System.out.print("Enter an output file full path: ");
        options.OutputFilePath = reader.readLine();

        System.out.println("Applying label to file...");

        // The privileged AssignmentMethod is used when users label files, and can
        // override STANDARD.
        options.AssignmentMethod = AssignmentMethod.PRIVILEGED;

        // Information on datastate is used to populate audit information. This field
        // doesn't impact the SDK behavior.
        options.DataState = DataState.REST;
        
        // Sets whether the sample should generate an audit event.
        options.IsAuditDiscoveryEnabled = true;

        // Apply the chosen label to the input file.

        boolean result = lblMgr.SetLabel(options);
        
        System.out.println("Applied label to file...");
        options.LabelId = reader.readLine();

        if (result) {
            // This section attempts to read the label from the file, if one was applied.
            System.out.println("Reading label from file...");

            options.InputFilePath = options.OutputFilePath;

            System.out.println("File Name: " + options.InputFilePath);
            System.out.println("File Label: " + lblMgr.GetLabel(options).label.getName());

            System.out.println("Reading owner from file...");
            System.out.println("File Label: " + lblMgr.GetProtection(options).getOwner());
        }

        else 
        {
            System.out.println("No changes were written to the input file.");
        }

        System.out.println("Press enter to quit.");
        reader.readLine();
    }

}
