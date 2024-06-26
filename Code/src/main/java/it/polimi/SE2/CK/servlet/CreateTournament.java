package it.polimi.SE2.CK.servlet;

import it.polimi.SE2.CK.DAO.TournamentDAO;
import it.polimi.SE2.CK.bean.SessionUser;
import it.polimi.SE2.CK.bean.Tournament;
import it.polimi.SE2.CK.utils.EmailManager;
import it.polimi.SE2.CK.utils.enumeration.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servlet that manage the creation of a tournament.
 */
@WebServlet("/CreateTournament")
@MultipartConfig
public class CreateTournament extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * A connection (session) with a specific database.
     */
    private Connection connection = null;


    /**
     * A convenience method which can be overridden so that there's no need to call super.init(config).
     *
     * @throws ServletException if an exception occurs that interrupts the servlet's normal operation
     */
    public void init() throws ServletException {
        try {
            ServletContext context=getServletContext();
            String driver = context.getInitParameter("dbDriver");
            String url = context.getInitParameter("dbUrl");
            String user = context.getInitParameter("dbUser");
            String password = context.getInitParameter("dbPassword");
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);

        } catch (ClassNotFoundException e) {
            throw new UnavailableException("Can't load database driver");
        } catch (SQLException e) {
            throw new UnavailableException("Couldn't get db connection");
        }
    }

    /**
     * Called by the server (via the service method) to allow a servlet to handle a GET request.
     *
     * @param request object that contains the request the client has made of the servlet
     * @param response object that contains the response the client has made of the servlet
     * @throws IOException if an input or output error is detected when the servlet handles the GET request
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("X-Frame-Options", "DENY"); //do not allow the page to be included in any frame or iframe
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains"); //your application should only be accessible via a secure connection (HTTPS)
        response.setHeader("Content-Security-Policy", "default-src 'self'"); //resources must come from the same source
        response.setHeader("X-Content-Type-Options", "nosniff"); //prevents browsers from interpreting files as anything other than their declared MIME type
        response.setHeader("X-XSS-Protection", "1; mode=block"); //block the page if an XSS attack is detected

        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        response.getWriter().println("Request non acceptable");

        String path = "ErrorPage.html";
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(path);
        try {
            requestDispatcher.forward(request, response);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called by the server (via the service method) to allow a servlet to handle a POST request.
     *
     * @param request object that contains the request the client has made of the servlet
     * @param response object that contains the response the client has made of the servlet
     * @throws IOException if an input or output error is detected when the servlet handles the GET request
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("X-Frame-Options", "DENY"); //do not allow the page to be included in any frame or iframe
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains"); //your application should only be accessible via a secure connection (HTTPS)
        response.setHeader("Content-Security-Policy", "default-src 'self'"); //resources must come from the same source
        response.setHeader("X-Content-Type-Options", "nosniff"); //prevents browsers from interpreting files as anything other than their declared MIME type
        response.setHeader("X-XSS-Protection", "1; mode=block"); //block the page if an XSS attack is detected

        HttpSession session = request.getSession();
        //the user is authorized or not - 401 error
        if(session.isNew() || session.getAttribute("user")==null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You can't access to this page");
            return;
        }

        String tournamentName = StringEscapeUtils.escapeHtml4(request.getParameter("tournamentNameInput"));
        String tournamentDescription = StringEscapeUtils.escapeHtml4(request.getParameter("tournamentDescriptionInput"));
        String registrationDeadline = StringEscapeUtils.escapeHtml4(request.getParameter("tournamentRegistrationDeadlineInput"));

        //400 error
        if (StringUtils.isAnyEmpty(tournamentName, registrationDeadline)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("All fields with an asterisk are required");
            return;
        }
        if (tournamentName.length()>45){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The max length of tournament name is 45 character");
            return;
        }
        if (tournamentDescription.length()>200){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("The max length of tournament name is 200 character");
            return;
        }

        //valid date
        //400 error
        if (registrationDeadline.length()>16){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("You must insert a valid date");
            return;
        }

        //transform string in date
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date parseDate;
        //400 error
        try{
            parseDate = dateTimeFormatter.parse(registrationDeadline+":00");
        } catch (ParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Insert a valid data");
            return;
        }
        Timestamp tournamentRegistrationDeadline = new Timestamp(parseDate.getTime());

        //get the actual date
        Date currentDate = new Date();
        Timestamp currentTimestamp = new Timestamp(currentDate.getTime());
        //400 error
        if (tournamentRegistrationDeadline.before(currentTimestamp)){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("You must insert a date after now");
            return;
        }

        TournamentDAO tournamentDAO=new TournamentDAO(connection);
        //500 error
        try {
            //409 error
            if (!tournamentDAO.checkTournamentByName(tournamentName)){
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().println("Existing tournament name, choose another one");
                return;
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("The server do not respond");
            return;
        }

        //get session user
        SessionUser user = (SessionUser) session.getAttribute("user");

        if (user.getRole()!= UserRole.EDUCATOR.getValue()){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You can't do this action");
            return;
        }

        //sets the new tournament data
        Tournament tournament=new Tournament();
        tournament.setCreatorId(user.getId());
        tournament.setCreatorUsername(user.getUsername());
        tournament.setName(tournamentName);
        tournament.setDescription(tournamentDescription);
        tournament.setRegDeadline(tournamentRegistrationDeadline);

        //creation tournament on DB
        //500 error
        try {
            tournamentDAO.createTournament(tournament);
        }
        catch (SQLException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("The server do not respond");
            return;
        }

        //200 ok
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        //send email to all student
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() ->
                EmailManager.sendEmailToAllStudentNewTournamentCreated(tournament.getCreatorUsername(), tournament.getRegDeadline(), connection));
        executor.shutdownNow();
    }

}
