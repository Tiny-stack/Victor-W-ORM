package org.victorworm;


public abstract class Model
{
	public Model()
	{
		
	}
	//this must be override in child class
    public static String tablename;
	public abstract Integer getId();
	// public abstract Map<String,Object> getFields();
	public abstract String getTableName();
	
	

}

