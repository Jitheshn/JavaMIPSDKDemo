package com.example.mip;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.microsoft.informationprotection.ApplicationInfo;
import com.microsoft.informationprotection.CacheStorageType;
import com.microsoft.informationprotection.ContentLabel;
import com.microsoft.informationprotection.Identity;
import com.microsoft.informationprotection.Label;
import com.microsoft.informationprotection.LogLevel;
import com.microsoft.informationprotection.MIP;
import com.microsoft.informationprotection.MipComponent;
import com.microsoft.informationprotection.MipConfiguration;
import com.microsoft.informationprotection.MipContext;
import com.microsoft.informationprotection.ProtectionDescriptor;
import com.microsoft.informationprotection.file.FileEngineSettings;
import com.microsoft.informationprotection.file.FileProfileSettings;
import com.microsoft.informationprotection.file.IFileEngine;
import com.microsoft.informationprotection.file.IFileHandler;
import com.microsoft.informationprotection.file.IFileProfile;
import com.microsoft.informationprotection.file.LabelingOptions;
import com.microsoft.informationprotection.file.ProtectionSettings;
import com.microsoft.informationprotection.internal.callback.FileHandlerObserver;

public class LabelManager {
	
	AuthDelegateImpl authDelegate;    
    IFileProfile fileProfile;
    IFileEngine fileEngine;
    MipContext mipContext;
    String userName;
    public LabelManager(ApplicationInfo appInfo, String userName)throws InterruptedException, ExecutionException
    {
        this.userName = userName;
        authDelegate = new AuthDelegateImpl(appInfo);
        
        // Initialize MIP For File SDK components.        
        MIP.initialize(MipComponent.FILE, "<SDK Paht>\\mip_sdk_java_wrappers_win32_1.15.86\\bins\\debug\\amd64");

        // Create MIP Configuration
        // MIP Configuration can be used to set various delegates, feature flags, and other SDK behavior. 
        MipConfiguration mipConfiguration = new MipConfiguration(appInfo, "mip_data", LogLevel.TRACE, false);
        
        // Create MipContext from MipConfiguration
        mipContext = MIP.createMipContext(mipConfiguration);
        
        // Create the FileProfile and Engine.
        fileProfile = CreateFileProfile();
        fileEngine = CreateFileEngine(fileProfile);
    }


    private IFileProfile CreateFileProfile() throws InterruptedException, ExecutionException
    {
        // The ConsentDelegate is required for all FileProfiles, but fires only when connecting to AD RMS.
        ConsentDelegate consentDelegate = new ConsentDelegate();

        // Create FileProfileSettings, passing in to LoadFileProfileAsync() and getting the result. 
        FileProfileSettings fileProfileSettings = new FileProfileSettings(mipContext, CacheStorageType.ON_DISK, consentDelegate);   
        
        Future<IFileProfile> fileProfileFuture = MIP.loadFileProfileAsync(fileProfileSettings);
        IFileProfile fileProfile = fileProfileFuture.get();
        
        return fileProfile;    
        
    }

    private IFileEngine CreateFileEngine(IFileProfile profile) throws InterruptedException, ExecutionException
    {                    
        // Create the file engine, passing in the username as the first parameter.
        // This sets the engineId to the username, making it easier to load the cached engine. 
        // Using cached engines reduces service road trips and will use cached use licenses for protected content.
        FileEngineSettings engineSettings = new FileEngineSettings(userName, authDelegate, "", "en-US");
     
        // Uncomment to set a functionality filter. These filters are useful for filtering or including labels based on protection type.
        // The example below will result in removal of labels with user-defined permissions. 
        //engineSettings.configureFunctionality(FunctionalityFilterType.CUSTOM, false);      
          
        // Set the user identity for the engine. This aids in service discovery.
        engineSettings.setIdentity(new Identity(userName));                
   
        // Add the engine and get the result. 
        Future<IFileEngine> fileEngineFuture = fileProfile.addEngineAsync(engineSettings);
        IFileEngine fileEngine = fileEngineFuture.get();
        return fileEngine;
    }

    private IFileHandler CreateFileHandler(FileOptions options, IFileEngine engine) throws InterruptedException, ExecutionException
    {
        // Create a FileHandler. FileHandlers are used to perform all file-specific operations. 
        FileHandlerObserver observer = new FileHandlerObserver();
        Future<IFileHandler> handlerFuture = engine.createFileHandlerAsync(options.InputFilePath, options.InputFilePath, options.GenerateChangeAuditEvent, observer, null);
        return handlerFuture.get();
    }

    public void ListLabels()
    {
        // Use the FileEngine to get all labels for the user and display on screen. 
        Collection<Label> labels = fileEngine.getSensitivityLabels();
        labels.forEach(label -> { 
            System.out.println(label.getName() + " : " + label.getId());
            if(label.getChildren().size() > 0)
            {
                label.getChildren().forEach(child -> {                
                    System.out.println("\t" + child.getName() + " : " + child.getId());
                });
            }
        });
    }

    public boolean SetLabel(FileOptions options) throws InterruptedException, ExecutionException
    {
    	IFileHandler fileHandler = null;
    	for(int i =0; i< 10; i++)
    	{
        // Create a new FileHandler for the specified file and options.        
    		fileHandler = CreateFileHandler(options, fileEngine);
    		
    		//Thread.sleep(100);
    		
    	}
        
        

        // LabelingOptions is used to set specific attributes about the labeling operation.
        // If the labeling operations throws a JustificationRequiredException, use the 
        // setJustificationMessage() and isDowngradeJustified() then retry. 
        LabelingOptions labelingOptions = new LabelingOptions();                    
        labelingOptions.setAssignmentMethod(options.AssignmentMethod);

        //labelingOptions.isDowngradeJustified(true);
        //labelingOptions.setJustificationMessage("My Justification Message");

        Label label = fileEngine.getLabelById(options.LabelId);

        // Attempt to set the label on the FileHandler.
        // The ProtectionSettings object can be used to write protection as another user
        // or to change the pfile extension behavior.                         
        fileHandler.setLabel(label, labelingOptions, new ProtectionSettings());

        // Check to see if handler has been modified. If not, skip commit. 
        boolean result = false;
        if(fileHandler.isModified())
        {
            // Commit the result. Will return false if no changes were made. 
            // Given that it's gated on the isModified() property, this should always be true.
            result = fileHandler.commitAsync(options.OutputFilePath).get();
        }

        return result;
    }

    public ContentLabel GetLabel(FileOptions options) throws InterruptedException, ExecutionException
    {
        // Create a FileHandler then get the label from the handler. 
        IFileHandler fileHandler = CreateFileHandler(options, fileEngine);
        return fileHandler.getLabel();
    }

    public ProtectionDescriptor GetProtection(FileOptions options) throws InterruptedException, ExecutionException
    {
        // Create a FileHandler then get the protection descriptor from the handler. 
        IFileHandler fileHandler = CreateFileHandler(options, fileEngine);
        return fileHandler.getProtection().getProtectionDescriptor();
    }

}
