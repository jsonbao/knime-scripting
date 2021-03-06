package de.mpicbg.knime.scripting.matlab.open;

import matlabcontrol.MatlabProxy;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

import de.mpicbg.knime.scripting.core.exceptions.KnimeScriptingException;
import de.mpicbg.knime.scripting.matlab.AbstractMatlabScriptingNodeModel;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabCode;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabFileTransfer;
import de.mpicbg.knime.scripting.matlab.ctrl.MatlabTable;

public class OpenInMatlabNodeModel extends AbstractMatlabScriptingNodeModel {
	
	/** proxy to connect to the Matlab instance */
	private MatlabProxy proxy;
	
	/**
     * Constructor
     */
	protected OpenInMatlabNodeModel() {
		// Define the ports and use a hash-map for setting models 
        super(createPorts(1), createPorts(0));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] executeImpl(PortObject[] inData,
			ExecutionContext exec) throws Exception {
		// Get the table
        BufferedDataTable data = (BufferedDataTable) inData[0];
        
        try {               	
        	// Instantiate the table parser
			table = new MatlabTable(data);
			
			if (tableTransferMethod.equals("file")) {
				// Transfer the KNIME table as hash map object dump to the JVM temp-folder
		        table.writeHashMapToTempFolder();
		        
		        // Prepare the MATLAB parser script
		        parserFile = new MatlabFileTransfer(AbstractMatlabScriptingNodeModel.MATLAB_HASHMAP_SCRIPT);
		        
		        // Compile the command to open the data in MATLAB
		        String cmd = MatlabCode.getOpenInMatlabCommand(matlabWorkspaceType, 
		        		parserFile.getPath(), 
		        		table.getHashMapTempPath());
		        
		        // Execute
		        proxy = matlabConnector.acquireProxyFromQueue();
		        proxy.eval(cmd);	        
			} else if (tableTransferMethod.equals("workspace")) {
				proxy = matlabConnector.acquireProxyFromQueue();
				table.pushTable2MatlabWorkspace(proxy, matlabWorkspaceType);
				proxy.eval(MatlabCode.getOpenMessage(matlabWorkspaceType));
			}
        	exec.checkCanceled();
        	
        	// Housekeeping
        	cleanup();
        } catch (Exception e) {
        	throw e;
    	} finally {
    		// Return the MATLAB proxy
    		if ((matlabConnector != null) && (proxy != null))
    			matlabConnector.returnProxyToQueue(proxy);
    	}
        
        logger.info("The data is now loaded in MATLAB. Switch to the MATLAB command window.");

        return new BufferedDataTable[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void openIn(PortObject[] inData, ExecutionContext exec)
			throws KnimeScriptingException {
		// nothing to do here
	}	
}
