package net.ssehub.sparkyservice.db.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import net.ssehub.sparkyservice.db.user.Password;
import net.ssehub.sparkyservice.db.user.StoredUser;
import net.ssehub.sparkyservice.db.user.PersonalSettings;
/**
 * 
 * Helper class for handling a single object of {@link SessionFactory}.
 * 
 * @author marcel
 */
public class SessionFactoryHelper {
    
    public static Boolean DEBUG_ENABLE = Boolean.TRUE; // TODO set in config fle
    
    /**
     * Will initialized in static init.
     */
    public static final SessionFactory SESSION_FACTORY;

    /**
     * All wanted annotated classes for the session factory which should be set before starting this application.
     * This should be a type of {@link AnnotatedClass}.
     * As mentioned in the hibernate documentation, this is a very heavy object why this is only build once inside 
     * the static initializer.
     */
    private static final Class<?>[] ANNOTATED_CLASSES = { 
            Password.class, 
            PersonalSettings.class, 
            StoredUser.class };
    
    /**
     * Creates a new {@link SessionFactory} and sets all anotatedClasses to the session factory.
     */
    static {
        try {
            Configuration cf = new Configuration().configure();
            for (Class<?> cls : ANNOTATED_CLASSES) {
                  cf.addAnnotatedClass((Class<AnnotatedClass>) cls);
            }
            final ServiceRegistry reg = new StandardServiceRegistryBuilder().applySettings(cf.getProperties()).build();
            SESSION_FACTORY = cf.buildSessionFactory(reg);
        } catch (Throwable e) {
            System.err.println("Error in creating SessionFactory object." + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

   public static void shutdown() {
       SESSION_FACTORY.close();
   }
}
