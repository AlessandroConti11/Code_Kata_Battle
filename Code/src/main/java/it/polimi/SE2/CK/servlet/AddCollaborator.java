package it.polimi.SE2.CK.servlet;

import it.polimi.SE2.CK.DAO.TournamentDAO;
import it.polimi.SE2.CK.DAO.UserDAO;
import it.polimi.SE2.CK.bean.SessionUser;
import it.polimi.SE2.CK.bean.Tournament;
import it.polimi.SE2.CK.utils.EmailManager;
import it.polimi.SE2.CK.utils.enumeration.TournamentState;
import it.polimi.SE2.CK.utils.enumeration.UserRole;
import jakarta.mail.MessagingException;
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
import java.util.Arrays;
import java.util.List;

/**
 * Servlet that manage the addition of collaborator to a tournament.
 */
@WebServlet("/AddCollaborator")
@MultipartConfig
public class AddCollaborator extends HttpServlet {
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

        SessionUser user = (SessionUser) session.getAttribute("user");
        String[] collaboratorsList = request.getParameterValues("collaboratorInput");
        //user is an educator
        //401 error
        if (user.getRole() != UserRole.EDUCATOR.getValue()){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("You can't do this action");
            return;
        }

        //existence of tournament
        TournamentDAO tournamentDAO = new TournamentDAO(connection);
        Tournament tournament = new Tournament();
        try {
            tournament.setId(Integer.parseInt(StringEscapeUtils.escapeHtml4(request.getParameter("TournamentId"))));
        }catch (Exception e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Internal error with the page, please try again");
            return;
        }

        if (tournament.getId()<=0){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Internal error with the page, please try again");
            return;
        }

        //500 error
        try {
            tournament = tournamentDAO.showTournamentById(tournament.getId());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("The server do not respond");
            return;
        }

        //tournament is in not in Closed phase
        //406 error
        if (tournament.getPhase().equals(TournamentState.CLOSED.getValue())){
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            response.getWriter().println("The tournament has already been closed");
            return;
        }

        //user is in the tournament
        //500 error
        try {
            //401 error
            if (!tournamentDAO.checkUserInTournament(tournament.getId(), user.getId())){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("You can't do this action");
                return;
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("The server do not respond");
            return;
        }

        if (collaboratorsList==null || collaboratorsList.length==0){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("You have to choose a collaborator");
            return;
        }
        //collaborator list
        List<String> listItems =Arrays.asList(collaboratorsList);
        List<Integer> collaborators = listItems.stream()
                .map(Integer::parseInt)
                .toList();

        //empty collaborator list
        //400 error


        UserDAO userDAO = new UserDAO(connection);
        //500 error
        for (Integer i : collaborators) {
            try {
                //selected collaborator is an educator
                //409 error
                Boolean res=userDAO.checkRole(i);
                if (res==null){
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.getWriter().println("The user you have choose doesn't exist");
                    return;
                }
                else if(res){
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.getWriter().println("The chosen user must be an educator");
                    return;
                }
                //selected collaborator not is in the tournament
                //409 error
                if (tournamentDAO.checkUserInTournament(tournament.getId(), i)){
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.getWriter().println("You have selected an educator already in the tournament");
                    return;
                }
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("The server do not respond");
                return;
            }
        }

        //add collaborator
        //500 error
        try {
            tournamentDAO.addCollaborator(tournament.getId(), collaborators);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("The server do not respond");
            return;
        }


        //200 ok
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }
}
