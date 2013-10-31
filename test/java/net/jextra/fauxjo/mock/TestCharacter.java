package net.jextra.fauxjo.mock;

import net.jextra.fauxjo.FauxjoField;
import net.jextra.fauxjo.FauxjoImpl;
import net.jextra.fauxjo.FauxjoPrimaryKey;
import java.util.Date;

public class TestCharacter extends FauxjoImpl
{

    @FauxjoPrimaryKey
    @FauxjoField( value = "characterId", defaultable = true )
    private Long id;

    @FauxjoField( "name" )
    private String name;

    @FauxjoField( "email" )
    private String email;

    @FauxjoField( value = "dateCreated", defaultable = true )
    private Date dateCreated;

    public TestCharacter()
    {
    }

    public TestCharacter( String name, String email )
    {
        this.name = name;
        this.email = email;
    }

    public Long getId()
    {
        return id;
    }

    public Date getDateCreated()
    {
        return dateCreated;
    }

    public void setDateCreated( Date dateCreated )
    {
        this.dateCreated = dateCreated;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }
}
