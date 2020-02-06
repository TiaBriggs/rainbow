package auxtestlib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that runs a command and captures its output.
 */
public class CommandRunner {
	/**
	 * Class that represents the output of a command.
	 */
	public static class CommandOutput {
		/**
		 * Output generated by the command.
		 */
		public String output;

		/**
		 * Error messages generated.
		 */
		public String error;

		/**
		 * Output generated by the command (in bytes).
		 */
		public byte outputBytes[];

		/**
		 * Output written by the commandin the stderr (in bytes).
		 */
		public byte errorBytes[];

		/**
		 * Program's exit code.
		 */
		public int exitCode;

		/**
		 * Has the program timed out?
		 */
		public boolean timedOut;
	}

	/**
	 * Creates a new instance.
	 */
	public CommandRunner() {
		/*
		 * Nothing to do.
		 */
	}

	/**
	 * Runs a command and captures the output. This method will return
	 * immediately (the process keeps running in the background). The process
	 * can me monitored and accessed through the {@link ProcessInterface} class.
	 * @param cmds the command and its arguments
	 * @param directory the directory where the command should bd executed
	 * @param limit execution time limit (in seconds)
	 * @return an interface to control the process
	 * @throws IOException failed to launch the process
	 */
	public ProcessInterface run_command_async(String[] cmds, File directory,
			int limit) throws IOException {
		if (cmds == null) {
			throw new IllegalArgumentException("cmds == null");
		}

		if (directory == null) {
			throw new IllegalArgumentException("directory == null");
		}

		if (limit <= 0) {
			throw new IllegalArgumentException("limit <= 0");
		}

		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(directory);
		Process p = pb.start();
		return ProcessInterface.makeProcessInterface(p, limit);
	}

	/**
	 * @param cmds deprecated
	 * @param directory deprecated
	 * @param limit deprecated
	 * @return deprecated
	 * @throws IOException deprecated
	 * @deprecated use {@link #run_command_async(String[], File, int)}
	 */
	@Deprecated
	public ProcessInterface runCommandAsync(String[] cmds, File directory,
			int limit) throws IOException {
		return run_command_async(cmds, directory, limit);
	}
	
	/**
	 * This method is a shortcut for the
	 * {@link #run_command_async(String[], File, int)}. It will invoke the
	 * command, wait for it to run and returns the command's output.
	 * @param cmds the command and its arguments
	 * @param directory the directory where the command should bd executed
	 * @param limit execution time limit (in seconds)
	 * @return the commands output in the stdout
	 * @throws IOException failed to launch the process
	 */
	public CommandOutput run_command(String cmds[], File directory, int limit)
			throws IOException {
		ProcessInterface pi = runCommandAsync(cmds, directory, limit);

		/*
		 * Wait until the process dies.
		 */
		while (pi.isRunning()) {
			try {
				Thread.sleep(ProcessInterface.PROCESS_POLLING);
			} catch (InterruptedException e) {
				/*
				 * We'll ignore this.
				 */
			}
		}

		return pi.getOutput();
	}

	/**
	 * @param cmds deprecated
	 * @param directory deprecated
	 * @param limit deprecated
	 * @return deprecated
	 * @throws IOException deprecated
	 * @deprecated use {@link #run_command(String[], File, int)}
	 */
	@Deprecated
	public CommandOutput runCommand(String cmds[], File directory, int limit)
			throws IOException {
		return run_command(cmds, directory, limit);
	}
	
	/**
	 * Thread that keeps reading an input stream and saves the output. The
	 * thread will automatically stop when the stream is closed.
	 */
	static class Capturer extends Thread {
		/**
		 * The input stream.
		 */
		private final InputStream m_input_stream;

		/**
		 * Buffer where the text is kept.
		 */
		private final StringBuffer m_result;

		/**
		 * Data read from the stream (without text conversion).
		 */
		private final ByteArrayOutputStream m_result_bytes;

		/**
		 * Creates and starts the thread.
		 * 
		 * @param is the stream to read
		 */
		Capturer(InputStream is) {
			assert is != null;

			m_input_stream = is;
			m_result = new StringBuffer();
			m_result_bytes = new ByteArrayOutputStream();
			start();
		}

		@Override
		public void run() {
			int read;
			try {
				while ((read = m_input_stream.read()) != -1) {
					synchronized (this) {
						m_result_bytes.write(read);
						m_result.append((char) read);
					}
				}
			} catch (IOException e) {
				/*
				 * We'll ignore I/O exceptions.
				 */
			}
		}

		/**
		 * Obtains a copy of the captured text.
		 * 
		 * @return the text
		 */
		synchronized String text() {
			return m_result.toString();
		}

		/**
		 * Obtains a copy of the captured bytes.
		 * 
		 * @return the captured bytes
		 */
		synchronized byte[] bytes() {
			return m_result_bytes.toByteArray();
		}
	}

	/**
	 * Interface provided to access the process while it is running.
	 */
	public static class ProcessInterface {
		/**
		 * Polling interval to check whether the process has finished (in
		 * milliseconds).
		 */
		private static final int PROCESS_POLLING = 200;

		/**
		 * The process itself.
		 */
		private final Process m_process;

		/**
		 * Is the process still running?
		 */
		private boolean m_running;

		/**
		 * What was the exit code for the process?
		 */
		private int m_exit_code;

		/**
		 * Stdout capturer.
		 */
		private final Capturer m_out;

		/**
		 * Stderr capturer.
		 */
		private final Capturer m_err;

		/**
		 * Has the program timed out?
		 */
		private boolean m_timed_out;

		/**
		 * Listeners of the process interface.
		 */
		private final List<ProcessInterfaceListener> m_listeners;

		/**
		 * Creates a new interface for the process. These objects are linked to
		 * their respective runners.
		 * 
		 * @param process the process that is running
		 * @param limit the time limit to run the program (in seconds).
		 * 
		 * @return the process interface
		 */
		private static ProcessInterface makeProcessInterface(Process process,
				int limit) {
			return new ProcessInterface(process, limit);
		}

		/**
		 * Creates a new interface for the process. These objects are linked to
		 * their respective runners.
		 * 
		 * @param process the process that is running
		 * @param limit the time limit to run the program (in seconds).
		 */
		private ProcessInterface(Process process, int limit) {
			assert process != null;

			this.m_process = process;
			m_running = true;
			m_exit_code = 0;
			m_out = new Capturer(process.getInputStream());
			m_err = new Capturer(process.getErrorStream());
			m_listeners = new ArrayList<>();
			m_timed_out = false;

			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized (ProcessInterface.this) {
						update_state();
						if (!m_running) {
							cancel();
						}
					}
				}
			}, PROCESS_POLLING, PROCESS_POLLING);

			final TimerTask timeout_task = new TimerTask() {
				@Override
				public void run() {
					synchronized (this) {
						if (killProcess()) {
							m_timed_out = true;
						}
					}
				}
			};

			addProcessInterfaceListener(new ProcessInterfaceListener() {
				@Override
				public void processFinished(ProcessInterface process) {
					timeout_task.cancel();
				}
			});

			timer.schedule(timeout_task, limit * 1000);
		}

		/**
		 * Adds a listener to the process interface.
		 * @param listener the listener
		 */
		public synchronized void add_process_interface_listener(
				ProcessInterfaceListener listener) {
			if (listener == null) {
				throw new IllegalArgumentException("listener == null");
			}

			m_listeners.add(listener);
		}

		/**
		 * @param listener deprecated
		 * @deprecated use
		 * 	{@link #add_process_interface_listener(ProcessInterfaceListener)}
		 */
		@Deprecated
		public synchronized void addProcessInterfaceListener(
				ProcessInterfaceListener listener) {
			add_process_interface_listener(listener);
		}

		/**
		 * Removes a listener from the process interface.
		 * @param listener the listener
		 */
		public synchronized void remove_process_interface_listener(
				ProcessInterfaceListener listener) {
			if (listener == null) {
				throw new IllegalArgumentException("listener == null");
			}

			m_listeners.remove(listener);
		}

		/**
		 * @param listener deprecated
		 * @deprecated use
		 * {@link #remove_process_interface_listener(ProcessInterfaceListener)}
		 */
		@Deprecated
		public synchronized void removeProcessInterfaceListener(
				ProcessInterfaceListener listener) {
			remove_process_interface_listener(listener);
		}
		
		/**
		 * Updates the state of the process. Since the
		 * <code>java.lang.Process</code> class doesn't provide any way of
		 * observing its state, we must probe regularly. This method should be
		 * called for that purpose.
		 */
		private synchronized void update_state() {
			if (!m_running) {
				return;
			}

			try {
				m_exit_code = m_process.exitValue();
				m_running = false;
				for (ProcessInterfaceListener l : new ArrayList<>(
						m_listeners)) {
					l.processFinished(this);
				}
			} catch (IllegalThreadStateException e) {
				/*
				 * Process is still running.
				 */
			}
		}

		/**
		 * Requests the process to be killed (if it is running).
		 * 
		 * @return was the process killed (<code>true</code>) or was it already
		 * dead (<code>false</code>)?
		 */
		public synchronized boolean killProcess() {
			if (!m_running) {
				return false;
			}

			m_process.destroy();
			while (true) {
				try {
					m_process.exitValue();
					break;
				} catch (IllegalThreadStateException e) {
					/*
					 * The process is still running.
					 */
				}
			}

			update_state();
			assert !m_running;
			return true;
		}

		/**
		 * Determines whether the process is still running.
		 * 
		 * @return is the process running?
		 */
		public synchronized boolean isRunning() {
			return m_running;
		}

		/**
		 * Obtains the output of the command. Can only be invoked if the process
		 * has been stopped.
		 * 
		 * @return the output
		 * 
		 * @throws IllegalStateException if the process is still running
		 */
		public synchronized CommandOutput getOutput() {
			if (m_running) {
				throw new IllegalStateException("Process still running.");
			}

			CommandOutput co = new CommandOutput();

			co.exitCode = m_exit_code;
			co.output = m_out.text();
			co.error = m_err.text();
			co.outputBytes = m_out.bytes();
			co.errorBytes = m_err.bytes();
			co.timedOut = m_timed_out;
			return co;
		}

		/**
		 * Obtains the text currently written to the stdout by the process.
		 * 
		 * @return the text written
		 */
		public synchronized String getOutputText() {
			return m_out.text();
		}

		/**
		 * Obtains the text currently written to the stderr by the process.
		 * 
		 * @return the text written
		 */
		public synchronized String getErrorText() {
			return m_err.text();
		}
	}

	/**
	 * Interface implemented by classes that observe a process interface.
	 */
	interface ProcessInterfaceListener {
		/**
		 * The process has finished.
		 * 
		 * @param process the process
		 */
		void processFinished(ProcessInterface process);
	}
}
