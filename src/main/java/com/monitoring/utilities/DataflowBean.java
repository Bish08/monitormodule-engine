package com.monitoring.utilities;

/**
 * @author matcon
 */
public class DataflowBean {
	private long id;
	private String description;
	private Integer trigger_type;
	private Integer frequency;
	private String last_run_date;
	private String dataflow_load_date;
	private Integer status;
	private String dataflow_dependencies;
	private String regex;

	private Integer number_files;
	private String folder;
	private String dataflow_id;
	private String tasps_id;
	private String description_status;
	private Integer delete_status;
	private String delete_date;
	private Integer auto_increment_date;
	private String last_end_run_date;
	private Integer is_last;
	private String value_pack;
	
	public DataflowBean() {
	}

	public DataflowBean(long id) {
		this.id = id;
	}

	public DataflowBean(long id, String description, Integer trigger_type, Integer frequency, String last_completed_date,
			String dataflow_load_date, Integer status, String regex, Integer number_files, String dataflow_dependencies, 
			String folder, String dataflow_id, String tasps_id, String description_status, Integer delete_status, String delete_date,Integer auto_increment_date,String last_end_run_date) {
		super();
		this.id = id;
		this.description = description;
		this.trigger_type = trigger_type;
		this.frequency = frequency;
		this.last_run_date = last_completed_date;
		this.dataflow_load_date = dataflow_load_date;
		this.status = status;
		this.last_run_date = last_completed_date;
		this.dataflow_load_date = dataflow_load_date;
		this.status = status;
		this.regex = regex;
		this.number_files = number_files;
		this.dataflow_dependencies = dataflow_dependencies;
		this.folder = folder;
		this.dataflow_id= dataflow_id;
		this.description_status = description_status;
		this.delete_status = delete_status;
		this.delete_date= delete_date;
		this.auto_increment_date=auto_increment_date;
		this.last_end_run_date = last_end_run_date;

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Integer getDelete_status() {
		return delete_status;
	}

	public void setDelete_status(Integer delete_status) {
		this.delete_status = delete_status;
	}

	public String getDelete_date() {
		return delete_date;
	}

	public void setDelete_date(String delete_date) {
		this.delete_date = delete_date;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getTrigger_type() {
		return trigger_type;
	}

	public void setTrigger_type(Integer trigger_type) {
		this.trigger_type = trigger_type;
	}

	public Integer getFrequency() {
		return frequency;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}

	public String getLast_run_date() {
		return last_run_date;
	}

	public void setLast_run_date(String last_run_date) {
		this.last_run_date = last_run_date;
	}

	public String getDataflow_load_date() {
		return dataflow_load_date;
	}

	public void setDataflow_load_date(String dataflow_load_date) {
		this.dataflow_load_date = dataflow_load_date;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getDataflow_dependencies() {
		return dataflow_dependencies;
	}

	public void setDataflow_dependencies(String dataflow_dependencies) {
		this.dataflow_dependencies = dataflow_dependencies;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public Integer getNumber_files() {
		return number_files;
	}

	public void setNumber_files(Integer number_files) {
		this.number_files = number_files;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getDataflow_id() {
		return dataflow_id;
	}

	public void setDataflow_id(String dataflow_id) {
		this.dataflow_id = dataflow_id;
	}

	public String getTasps_id() {
		return tasps_id;
	}

	public void setTasps_id(String tasps_id) {
		this.tasps_id = tasps_id;
	}

	public String getDescription_status() {
		return description_status;
	}

	public void setDescription_status(String description_status) {
		this.description_status = description_status;
	}

	public Integer getAuto_increment_date() {
		return auto_increment_date;
	}

	public void setAuto_increment_date(Integer auto_increment_date) {
		this.auto_increment_date = auto_increment_date;
	}


	public String getLast_end_run_date() {
		return last_end_run_date;
	}

	public void setLast_end_run_date(String last_end_run_date) {
		this.last_end_run_date = last_end_run_date;
	}

	public Integer getIs_last() {
		return is_last;
	}

	public void setIs_last(Integer is_last) {
		this.is_last = is_last;
	}

	public String getValue_pack() {
		return value_pack;
	}

	public void setValue_pack(String value_pack) {
		this.value_pack = value_pack;
	}

	@Override
	public String toString() {
		return "DataflowBean [id=" + id + ", description=" + description + ", trigger_type=" + trigger_type
				+ ", frequency=" + frequency + ", last_run_date=" + last_run_date + ", dataflow_load_date="
				+ dataflow_load_date + ", status=" + status + ", dataflow_dependencies=" + dataflow_dependencies
				+ ", regex=" + regex + ", number_files=" + number_files + ", folder=" + folder + ", dataflow_id="
				+ dataflow_id + ", tasps_id=" + tasps_id + ", description_status=" + description_status
				+ ", delete_status=" + delete_status + ", delete_date=" + delete_date + ", auto_increment_date="
				+ auto_increment_date + ", getId()=" + getId() + ", getDelete_status()=" + getDelete_status()
				+ ", getDelete_date()=" + getDelete_date() + ", getDescription()=" + getDescription()
				+ ", getTrigger_type()=" + getTrigger_type() + ", getFrequency()=" + getFrequency()
				+ ", getLast_run_date()=" + getLast_run_date() + ", getDataflow_load_date()=" + getDataflow_load_date()
				+ ", getStatus()=" + getStatus() + ", getDataflow_dependencies()=" + getDataflow_dependencies()
				+ ", getRegex()=" + getRegex() + ", getNumber_files()=" + getNumber_files() + ", getFolder()="
				+ getFolder() + ", getDataflow_id()=" + getDataflow_id() + ", getTasps_id()=" + getTasps_id()
				+ ", getDescription_status()=" + getDescription_status() + ", getAuto_increment_date()="
				+ getAuto_increment_date() + ", hashCode()=" + hashCode() + ", getClass()=" + getClass()
				+ ", toString()=" + super.toString() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((auto_increment_date == null) ? 0 : auto_increment_date.hashCode());
		result = prime * result + ((dataflow_dependencies == null) ? 0 : dataflow_dependencies.hashCode());
		result = prime * result + ((dataflow_id == null) ? 0 : dataflow_id.hashCode());
		result = prime * result + ((dataflow_load_date == null) ? 0 : dataflow_load_date.hashCode());
		result = prime * result + ((delete_date == null) ? 0 : delete_date.hashCode());
		result = prime * result + ((delete_status == null) ? 0 : delete_status.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((description_status == null) ? 0 : description_status.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result + ((frequency == null) ? 0 : frequency.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((last_run_date == null) ? 0 : last_run_date.hashCode());
		result = prime * result + ((number_files == null) ? 0 : number_files.hashCode());
		result = prime * result + ((regex == null) ? 0 : regex.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((tasps_id == null) ? 0 : tasps_id.hashCode());
		result = prime * result + ((trigger_type == null) ? 0 : trigger_type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataflowBean other = (DataflowBean) obj;
		if (auto_increment_date == null) {
			if (other.auto_increment_date != null)
				return false;
		} else if (!auto_increment_date.equals(other.auto_increment_date))
			return false;
		if (dataflow_dependencies == null) {
			if (other.dataflow_dependencies != null)
				return false;
		} else if (!dataflow_dependencies.equals(other.dataflow_dependencies))
			return false;
		if (dataflow_id == null) {
			if (other.dataflow_id != null)
				return false;
		} else if (!dataflow_id.equals(other.dataflow_id))
			return false;
		if (dataflow_load_date == null) {
			if (other.dataflow_load_date != null)
				return false;
		} else if (!dataflow_load_date.equals(other.dataflow_load_date))
			return false;
		if (delete_date == null) {
			if (other.delete_date != null)
				return false;
		} else if (!delete_date.equals(other.delete_date))
			return false;
		if (delete_status == null) {
			if (other.delete_status != null)
				return false;
		} else if (!delete_status.equals(other.delete_status))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (description_status == null) {
			if (other.description_status != null)
				return false;
		} else if (!description_status.equals(other.description_status))
			return false;
		if (folder == null) {
			if (other.folder != null)
				return false;
		} else if (!folder.equals(other.folder))
			return false;
		if (frequency == null) {
			if (other.frequency != null)
				return false;
		} else if (!frequency.equals(other.frequency))
			return false;
		if (id != other.id)
			return false;
		if (last_run_date == null) {
			if (other.last_run_date != null)
				return false;
		} else if (!last_run_date.equals(other.last_run_date))
			return false;
		if (number_files == null) {
			if (other.number_files != null)
				return false;
		} else if (!number_files.equals(other.number_files))
			return false;
		if (regex == null) {
			if (other.regex != null)
				return false;
		} else if (!regex.equals(other.regex))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (tasps_id == null) {
			if (other.tasps_id != null)
				return false;
		} else if (!tasps_id.equals(other.tasps_id))
			return false;
		if (trigger_type == null) {
			if (other.trigger_type != null)
				return false;
		} else if (!trigger_type.equals(other.trigger_type))
			return false;
		return true;
	}



	

}
