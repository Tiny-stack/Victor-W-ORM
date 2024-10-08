# Victor-W-ORM
## about
- Victo-W-ORM is aims to create an extremelely lightweight ORM for sqlite db that aims to support native OS application.
- Usually available ORM library are heavy and has many feature that reuires in web based application while native OS application don't 
require many of them services.
- Victor-W-ORM aims to find a  solution for this problem to provide ORM functionality in extremely low configuration.
- Initially my design handles all the CRUD operation (Including nested model support) with just 2 Java classes. (CRUD->Generic java class and Model-> abstract Java Class)

## Features
- Fully functioning CRUD in ORM Style
- Get ride of writing sql queries
- Automatic feild creation based on the models
- Support of nested models
- Easy to configure and use, no Over head

## how to use victo-W-ORM in your project
- Victor-W-ORM is very easy to use in your project, that requires almost no configuration. Download the .jar and set it to your project build path.
- By default Victor-W-ORM creates a db named 'default.db' however user can explicitly call the static method ->changeDB(dbname) of CRUD class to change the db name(make sure your db contains the extension .db).
- If the db is not valid it will not throw any exception however method will return false and db name will default.db.

### Creating models
- To create models you must extend the Model class which is an abstract class make sure to override the abstract method
    - getId()
    - getTableName()
- Create getter setter for each field
- the method getTableName must return the table Name of the model. (Note; you don't have to create the table manually, it will be automatically created (lazily))


#### Example Model:

```
import java.sql.SQLException;
import org.victorworm.CRUD;
import org.victorworm.Model;
public class User extends Model
{
	private Integer id;
	private String name;
	private String dob;
	private String password;
	private String about;
	private User bestFriendId;


	@Override
	public Integer getId() {
		return this.id;
	}


	public void setId(Integer id) {
		this.id = id;
	}
	public User(String name,String dob,String password, String about,User bestFriendId)
	{
		this.name = name;
		this.dob = dob;
		this.password = password;
		this.about = about;
		this.bestFriendId = bestFriendId;
        this.id = 0;
	}
	@Override
	public String getTableName() {
		return "USER";
	}

	public User()
	{}
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getDob() {
		return dob;
	}


	public void setDob(String dob) {
		this.dob = dob;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getAbout() {
		return about;
	}


	public void setAbout(String about) {
		this.about = about;
	}


	public User getBestFriendId() {
		return bestFriendId;
	}

	public void setBestFriendId(User bestFriendId) {
		this.bestFriendId = bestFriendId;
	}


	// private Set<String> columns =Set.of("name","dob","password","about","best_friend_id"); 
	@Override
    public String toString()
    {
        return "Name: "+this.name+" id: "+this.id+"BESt friend: "+this.bestFriendId;
    }
}
```

#### Example Use of this model:

```
	public static void main(String[] args) throws SQLException
	{
        CRUD.changeDB("nicedb.db");
		CRUD<User> userCrud = new CRUD<>(User.class);
		User u = new User("Vishwajeet Rauniyar","12,3,4","09/10/2001","I am Tiny",null);
		User u2 = new User("Tiny","123","09/10/2001","I am his friend",u);
		userCrud.save(u2);
        System.out.println("u1: "+u);
        System.out.println("u2: "+u2);
	}
```

# Suggestions are Welcome


