package resources.services;

import resources.control.Service;

public class ServiceProblem {
	
	private Service service;
	private ProblemSeverity severity;
	private String error;
	
	public ServiceProblem(Service s, ProblemSeverity severity, String error) {
		this.service = s;
		this.severity = severity;
		this.error = error;
	}
	
	public Service getService() { return service; }
	public ProblemSeverity getSeverity() { return severity; }
	public String getError() { return error; }
	
}
