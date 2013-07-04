package agents;

import java.io.IOException;

import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.registry.Registry;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.Constants;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;

/**
Launcher for My agents. 
This will launch as many instances of each of the sample agents as possible, 
all using one connection.
 */
public class LaunchMyAgents {

	/**
	 * constants that will be used to instantiate each type of agent to the simulation
	 */
	private static final String FIRE_BRIGADE_FLAG = "-fb";
	private static final String POLICE_FORCE_FLAG = "-pf";
	private static final String AMBULANCE_TEAM_FLAG = "-at";
	private static final String CIVILIAN_FLAG = "-cv";
	
	private LaunchMyAgents() {}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.setLogContext("MyAgent");
		
		try {
			Registry.SYSTEM_REGISTRY.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
			Config config = new Config();
			args = CommandLineOptions.processArgs(args, config);
			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY, Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY, Constants.DEFAULT_KERNEL_HOST_NAME);
			int fb = -1;
			int pf = -1;
			int at = -1;
			// CHECKSTYLE:OFF:ModifiedControlVariable
			for (int i = 0; i < args.length; ++i) {
				
				if (args[i].equals(FIRE_BRIGADE_FLAG)) {
					fb = Integer.parseInt(args[++i]);
				}
				else if (args[i].equals(POLICE_FORCE_FLAG)) {
					pf = Integer.parseInt(args[++i]);
				}
				else if (args[i].equals(AMBULANCE_TEAM_FLAG)) {
					at = Integer.parseInt(args[++i]);
				}
				else {
					Logger.warn("Opção desconhecida: " + args[i]);
				}
			}
			// CHECKSTYLE:ON:ModifiedControlVariable
			ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
			connect(launcher, fb, pf, at, config);
		}
		catch (IOException e) {
			Logger.error("Erro conectando agentes", e);
		}
		catch (ConfigException e) {
			Logger.error("Erro de configuração", e);
		}
		catch (ConnectionException e) {
			Logger.error("Erro conectando agentes", e);
		}
		catch (InterruptedException e) {
			Logger.error("Erro conectando agentes", e);
		}
		
	}

	/**
	 * local function that makes the connection of specified agents to simulation
	 * @param launcher
	 * @param fb
	 * @param pf
	 * @param at
	 * @param config
	 * @throws InterruptedException
	 * @throws ConnectionException
	 */
	private static void connect(ComponentLauncher launcher, int fb, int pf, int at, Config config) throws InterruptedException, ConnectionException {
		int i = 0;
		
		
		try {
			while (fb-- != 0) {
				Logger.info("Conectando MyFireBrigade " + (i++) + "...");
				launcher.connect(new FireBrigadeAgent());
				Logger.info("Sucesso");
			}
		}
		catch (ComponentConnectionException e) {
			Logger.info("Falhou: " + e.getMessage());
		}
		
        try {
            while (pf-- != 0) {
                Logger.info("Connecting police force " + (i++) + "...");
                launcher.connect(new PoliceForceAgent());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
           
        
        try {
            while (at-- != 0) {
                Logger.info("Connecting ambulance team " + (i++) + "...");
                launcher.connect(new AmbulanceTeamAgent());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }
            
        /*
        try {
            while (true) {
                Logger.info("Connecting centre " + (i++) + "...");
                launcher.connect(new CenterAgent());
                Logger.info("success");
            }
        }
        catch (ComponentConnectionException e) {
            Logger.info("failed: " + e.getMessage());
        }*/
            /*
            try {
                while (true) {
                    Logger.info("Connecting dummy agent " + (i++) + "...");
                    launcher.connect(new DummyAgent());
                    Logger.info("success");
                }
            }
            catch (ComponentConnectionException e) {
                Logger.info("failed: " + e.getMessage());
            }*/
	}

}
