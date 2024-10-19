package org.worm;


import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// import com.google.gson.Gson;

// import org.sqlite.ExtendedCommand.SQLExtension;



// import org.sqlite.SQLiteException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;

public class CRUD<E extends Model> {
	private static String DB_URL = "jdbc:sqlite:default.db";
    private static final Logger logger = Logger.getLogger(CRUD.class.getName());
     private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.db$");
	private Connection connection;
	 private Class<E> clazz;
	private String tableName;
    public static boolean changeDB(String dbName){
         if (dbName == null || dbName.isEmpty() || dbName.length()>255 || !VALID_NAME_PATTERN.matcher(dbName).matches()) {
            logger.info("Invalid db Name found: Unable to overwrite dbname from default to "+dbName);
            return false;
        }

        
        CRUD.DB_URL = "jdbc:sqlite:"+dbName;
        return true;
    }
	public CRUD(Class<E> clazz) throws SQLException {
        this.clazz = clazz;
   
        try {
            // Create an instance of the class to invoke getTableName()
            E instance = clazz.getDeclaredConstructor().newInstance();
            Method me = clazz.getMethod("getTableName");
            String table = (String) me.invoke(instance); // Invoke method on instance, not clazz

            // Use the table name from the model
            this.tableName = table;

            if (!doesTableExist(tableName)) {
                    boolean created = createTable();
                    if(!created)
                        throw new SQLException("Unable to create the table "+this.tableName);
            }
            
            System.out.println("Connection stablished");

            
        } catch (Exception e) {
            throw new RuntimeException("Error initializing CRUD", e);
        }
    }
	private static String getterMethod(String field){
        if(field.equals(""))
            return "";
        char ch = field.charAt(0);
        field = field.substring(1,field.length());
        if(ch>90)
        {
            ch-=32;
        }
        return "get"+ch+field;

    }
    private static String setterMethod(String field){
        if(field.equals(""))
            return "";
        char ch = field.charAt(0);
        field = field.substring(1,field.length());
        if(ch>90)
        {
            ch-=32;
        }
        return "set"+ch+field;    
    }
	private Integer create(E object) throws SQLException,IllegalAccessException,NoSuchMethodException,InvocationTargetException{
       
		Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder("VALUES (");

        for (Field field : fields) {
            if(field.getName().equals("id"))
                continue;
            sql.append(field.getName()).append(", ");
            placeholders.append("?, ");
        }
        sql.delete(sql.length() - 2, sql.length()).append(") ");
        placeholders.delete(placeholders.length() - 2, placeholders.length()).append(")");

        String query = sql.append(placeholders).toString();
        System.out.println("Generated SQL: " + query);

        // Prepare the statement and set values dynamically
        PreparedStatement stmt = connection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
        int index = 1;
        for (Field field : fields) {
            if(field.getName().equals("id"))
                continue;
            System.out.println("Excuting for: "+field.getName());            
            Method getNameMethod = clazz.getMethod(getterMethod(field.getName()));
            System.out.println("getMethodName: "+getNameMethod);
            if(Model.class.isAssignableFrom(field.getType())) {
                Model nestedModel = (Model) getNameMethod.invoke(object);
                System.out.println("nestedModel: "+nestedModel);
                if (nestedModel != null) {
                    Integer nestedId = new CRUD<>(nestedModel.getClass()).save(nestedModel);
                    // Replace the nested model with its ID in the main object
                    // field.set(object, nestedId);
                    stmt.setInt(index, nestedId);
                }
                index++; //make sure even if nested class is null index incremented
                continue;
            }
            //here check if field is of type Model class and handle the nested Model.
            stmt.setObject(index++, getNameMethod.invoke(object));
            System.out.println("Excuted for: "+field.getName());            

        }

        // Execute the INSERT operation
        stmt.executeUpdate();
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) 
        {
            if (generatedKeys.next()) {
                int id = generatedKeys.getInt(1); // The generated ID
                return id;
            } else {
                System.out.println("No ID obtained.");
                return 0;
            }
        }
         catch(SQLException e) {
            System.out.println(e.getMessage());
            return 0;
            // System.out.println("Record inserted successfully.");
         }
	}
	private Integer update(E object) throws NoSuchMethodException,ClassCastException, SQLException,IllegalAccessException,InvocationTargetException,IllegalArgumentException
	{
      
        System.out.println("Here it comes for update: "+object);
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");

        for (Field field : fields) {
            if(!field.getName().equals("id"))
            {
                sql.append(field.getName()).append(" = ?, ");
            }
            // placeholders.append("?, ");
        }
        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE id = "+object.getId()); // it is safe to write here like this, since it will be alwasy number so no risk of SQL Injection here
        String query = sql.toString();
        System.out.println("Generated SQL: " + query);

        // Prepare the statement and set values dynamically
        PreparedStatement stmt = connection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
        int index = 1;
        for (Field field : fields) {
            if(field.getName().equals("id"))
                continue;
                System.out.println("Excuting for: "+field.getName());
            Method getNameMethod = clazz.getMethod(getterMethod(field.getName()));
            if(Model.class.isAssignableFrom(field.getType())) {
                Model nestedModel = (Model) getNameMethod.invoke(object);
                if (nestedModel != null) {
                    Integer nestedId = new CRUD<>(nestedModel.getClass()).save(nestedModel);
                    stmt.setInt(index, nestedId);
                }
                index++; //make sure even if nested class is null index incremented
                continue;
            }
            stmt.setObject(index++, getNameMethod.invoke(object));
            System.out.println("Excuted for: "+field.getName());
        }
        stmt.executeUpdate();
        
		return object.getId();
	}
    /**
     * Saves the specified model to the database.
     * <p>
     * This method will create a new record if the model does not already exist (i.e., if the model's ID is null or less than or equal to zero). 
     * If the model already exists, it will update the existing record.
     * </p>
     *
     * @param model the model to save or update; must not be null and should match the associated table name
     * @return the ID of the saved or updated model; returns 0 if the operation fails
     */
	public Integer save(Model model)
	{
        
        
         try
         {
            this.connection = SQLITEConnection.getConnection(DB_URL);
            @SuppressWarnings("unchecked")
            E object = (E)(model);
            if(!this.tableName.equals(model.getTableName()))
                throw new SQLException("CRUD BELONGS to another model");
            if((Integer)model.getId()==null || model.getId()<=0l)
            {
        
                    return this.create(object);
            }
            else{
                //UPDATE Operation
                return this.update(object);
                
            }
        }
        catch(InvocationTargetException ie)
        {
            logger.info("UNable to Invoke method: getter for id:-> getId() is not defined");//only for testing, in final version this should be logged instead of print
        }
        catch(IllegalAccessException ie)
        {
            logger.info("Illegal Access exption: "+ie);
        }
        catch(NoSuchMethodException ne)
        {
            logger.info("Getter or setter are missing: "+ne);
        }
        catch(SQLException se)
        {
            logger.info("SQLexception: "+se);
        }
        return 0;
	}
	
	private List<E> resultSetToObject(ResultSet rs) throws InstantiationException, IllegalAccessException,NoSuchMethodException, IllegalArgumentException, InvocationTargetException, SecurityException, SQLException {
            List<E> response = new ArrayList<>();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            Field fields[] = clazz.getDeclaredFields();
            Map<String,Field> nesteModelName = new HashMap<>();
            for(Field field:fields)
            {
                if(Model.class.isAssignableFrom(field.getType()))
                    nesteModelName.put(field.getName(),field);
            }
            int columnCount = rsMetaData.getColumnCount();
            
            // Iterate through the ResultSet rows
            while (rs.next()) {
                E instance = clazz.getDeclaredConstructor().newInstance(); // Create a new instance of E

                // Iterate over each column
                for (int i = 1; i <= columnCount; i++) {
                    if(rs.getObject(i)==null)
                        continue;
                    String columnName = rsMetaData.getColumnName(i);  // Get column name
                    String setterName = setterMethod(columnName); // Assuming standard naming conventions
                                // Get column value
                    try {
                        if(nesteModelName.containsKey(columnName))
                        {
                            Class<? extends Model> nestedModel = nesteModelName.get(columnName).getType().asSubclass(Model.class);
                            Model model = (Model) new CRUD<>(nestedModel).findById((Integer)rs.getObject(i));
                            Method setter = clazz.getMethod(setterName, model.getClass());
                            setter.invoke(instance,model);
                        }
                        else
                        {
                            Object columnValue = rs.getObject(i);
                            Method setter = clazz.getMethod(setterName, columnValue.getClass());
                            setter.invoke(instance, columnValue); // Invoke the setter method
                        }
                    } catch (NoSuchMethodException e) {
                        logger.info("No setter found for " + columnName);
                    }
                    
                }

                response.add(instance); 
            }

            return response;
        }
    /**
     * Retrieves a model object from the database based on the given ID.
     * <p>
     * This method executes a SQL query to find a record in the database that matches the specified ID.
     * If a record is found, it converts the result set to the appropriate model object and returns it.
     * </p>
     *
     * @param id the ID of the model to retrieve; must not be null
     * @return the model object associated with the given ID, or null if no record is found or an error occurs
     */
	public E findById(Integer id)
	{
        try
        {
            this.connection = SQLITEConnection.getConnection(DB_URL);
        
		    String sql = "SELECT * FROM " + this.tableName + " WHERE id = ?";
	        PreparedStatement preparedStatement = connection.prepareStatement(sql);
	        preparedStatement.setInt(1, id);
	        ResultSet resultSet = preparedStatement.executeQuery();
	        return resultSetToObject(resultSet).get(0);
	    } 
        catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null; 
	}
    /**
     * Retrieves all model objects from the database.
     * <p>
     * This method executes a SQL query to select all records from the corresponding table in the database.
     * It converts the resulting result set into a list of model objects and returns that list.
     * </p>
     *
     * @return a list of all model objects in the table; returns null if an error occurs during the retrieval
     */
	public List<E> findAll()
	{
        try
        {   
            this.connection = SQLITEConnection.getConnection(DB_URL);
		    String sql = "SELECT * FROM " + this.tableName;
	        PreparedStatement preparedStatement = connection.prepareStatement(sql);
	        ResultSet resultSet = preparedStatement.executeQuery();
	        return resultSetToObject(resultSet);
	    } 
        catch (Exception e) 
        {
	        e.printStackTrace();
	    }
	    return null; // Return null if not found
	}
    /**
     * Retrieves model objects from the database based on a specified column and its value.
     * <p>
     * This method executes a SQL query to select all records from the corresponding table where the specified
     * column matches the provided value. It converts the resulting result set into a list of model objects and returns that list.
     * </p>
     *
     * @param columnName the name of the column to filter the results by; must not be null or empty
     * @param value the value to match in the specified column; can be of any object type
     * @return a list of model objects matching the specified column value; returns null if an error occurs during the retrieval
     */
	public List<E> findByColumn(String columnName,Object value)
	{
        try
		{    
            this.connection = SQLITEConnection.getConnection(DB_URL);
            String sql = "SELECT * FROM " + this.tableName+" WHERE "+columnName+" = ?";
	        PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setObject(1, value);
	        ResultSet resultSet = preparedStatement.executeQuery();
	        return resultSetToObject(resultSet);
	    } 
        catch (Exception e) 
        {
	        logger.info("Exception in findByCOlumn: "+e);
	    }
	    return null; // Return null if not found
	}
    /**
     * Retrieves a paginated list of model objects from the database.
     * <p>
     * This method executes a SQL query to select all records from the corresponding table, applying pagination
     * based on the specified limit and offset. The results are converted from the resulting result set into a list of model objects.
     * </p>
     *
     * @param limit the maximum number of records to retrieve; must be a positive integer
     * @param offset the number of records to skip before starting to collect the result set; must be a non-negative integer
     * @return a list of model objects retrieved from the database; returns null if an error occurs during the retrieval
     */
	public List<E> findAll(Integer limit,Integer offset)
	{
        try
        {
                this.connection = SQLITEConnection.getConnection(DB_URL);
                String sql = "SELECT * FROM " + this.tableName+" LIMIT "+limit+" OFFSET "+offset;
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSetToObject(resultSet);
	    } 
        catch (Exception e) {
	        logger.info("FInALL des not executed properly: "+e);
	    }
	    return null; // Return null if not found
	}
    /**
     * Deletes a record from the database based on the specified ID.
     * <p>
     * This method executes a SQL DELETE statement to remove the record that matches the given ID
     * from the corresponding table. It checks that the operation is valid by ensuring that the 
     * CRUD instance is associated with the correct model. The method returns a boolean indicating 
     * whether the deletion was successful.
     * </p>
     *
     * @param id the ID of the record to be deleted; must be a positive integer
     * @return true if the record was successfully deleted; false otherwise
     */
	public boolean delete(Integer id)
    {
		 try
         {
            this.connection = SQLITEConnection.getConnection(DB_URL);
            String sql = "DELETE FROM " +this.tableName + " WHERE id = "+id;
		    
		     PreparedStatement preparedStatement = connection.prepareStatement(sql);
		        
		        int rowsAffected = preparedStatement.executeUpdate();
		        return rowsAffected > 0; // returns true if rows were deleted
		} catch (SQLException ex) {
		        logger.info("SQL error in delete: "+ex);
		        return false; // returns false if the delete operation failed
		    }
    }
    /**
     * Deletes a record from the database based on the specified model instance.
     * <p>
     * This method calls the {@link #delete(Integer)} method with the ID of the provided model instance.
     * It attempts to delete the record associated with the instance. If an SQL exception occurs,
     * it logs the error and returns false.
     * </p>
     *
     * @param e the model instance to be deleted; must not be null
     * @return true if the record was successfully deleted; false if the deletion failed
     */
	public boolean delete(E e) 
	{
        try
        {
		    return this.delete(e.getId());
        }
        catch(Exception sq)
        {
            logger.info("Unagel to delelet model of type: "+e.getClass()+" : for id = "+e.getId());
            return false;
        }
	}
    private boolean createTable() {
        try {
            this.connection = SQLITEConnection.getConnection(DB_URL);
            E dummyInstance = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = this.clazz.getDeclaredFields();
            StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName + " ( id INTEGER PRIMARY KEY AUTOINCREMENT,");
            
            // Loop through fields, skipping the 'id' field
            for (Field field : fields) {
                if (field.getName().equals("id")) {
                    continue; // skip the id field, it's already the primary key
                }
                // Get the corresponding getter method for the field
                Method getter = clazz.getMethod(getterMethod(field.getName()));
                if (field.getType().equals(Integer.class)) { // Integer fields
                    Integer defaultValue = (Integer) getter.invoke(dummyInstance);
                    sql.append(" ").append(field.getName()).append(" INTEGER");
                    if (defaultValue != null) {
                        sql.append(" DEFAULT ").append(defaultValue); // Add default value if not null
                    }
                    sql.append(",");
                } else if (field.getType().equals(String.class)) { // String fields
                    String defaultValue = (String) getter.invoke(dummyInstance);
                    sql.append(" ").append(field.getName()).append(" TEXT");
                    if (defaultValue != null) {
                        sql.append(" DEFAULT '").append(defaultValue).append("'"); // Add default value in quotes for strings
                    }
                    sql.append(",");
                } else if (Model.class.isAssignableFrom(field.getType())) { // Model fields (relationships)
                    Model mod = ((Model) getter.invoke(dummyInstance));
                    Integer defaultValue = null;
                    if(mod!=null)
                        defaultValue = mod.getId();
                    sql.append(" ").append(field.getName()).append(" INTEGER");
                    if (defaultValue != null) {
                        sql.append(" DEFAULT ").append(defaultValue);
                    }
                    sql.append(",");
                } else {
                    // Unsupported type
                    logger.warning("Field type not supported: " + field.getName() + " in class " + clazz.getName());
                }
            }
    
            // Remove the last trailing comma
            if (sql.charAt(sql.length() - 1) == ',') {
                sql.deleteCharAt(sql.length() - 1);
            }
    
            // Close the table definition
            sql.append(");");
    
            // Execute the SQL statement
            try (Statement stmt = this.connection.createStatement()) {
                stmt.execute(sql.toString());
                logger.info("Table created successfully: " + tableName);
            }
    
            return true; // Successfully created the table
        } catch (SQLException sq) {
            logger.severe("Unable to create table: " + this.tableName + " " + sq);
        } catch (ReflectiveOperationException e) {
            logger.severe("Reflection error in creating table: " + this.tableName + " " + e);
        }
    
        return false; // Failed to create the table
    }
    
	 private boolean doesTableExist(String tableName) {
	        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';";
	        
	        try 
            {
                this.connection = SQLITEConnection.getConnection(DB_URL);
                Statement statement = connection.createStatement();
	             ResultSet resultSet = statement.executeQuery(sql);
	            return resultSet.next(); // Returns true if the table exists
	        } catch (SQLException e) {
	            e.printStackTrace();
	            return false;
	        }
	    }
}