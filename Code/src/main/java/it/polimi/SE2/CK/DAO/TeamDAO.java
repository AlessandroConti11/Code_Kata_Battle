package it.polimi.SE2.CK.DAO;

import it.polimi.SE2.CK.bean.SessionUser;
import it.polimi.SE2.CK.utils.enumeration.TeamState;
import it.polimi.SE2.CK.utils.enumeration.TeamStudentState;
import it.polimi.SE2.CK.utils.enumeration.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO {
    private final Connection con;

    public TeamDAO(Connection con) {
        this.con = con;
    }

    /**
     * Gets the teams in a specific battle.
     *
     * @param battleId the specific battle.
     * @return the list of team in a specific battle.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<Integer> getTeamInBattle(int battleId) throws SQLException {
        //search query
        String query = "SELECT idteam " +
                "FROM team " +
                "WHERE battleId = ? and (not phase = ?)";
        //statement
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ArrayList<Integer> result = new ArrayList<>();

        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, battleId);
            preparedStatement.setString(2, TeamState.INCOMPLETE.getValue());
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                result.add(resultSet.getInt("idteam"));
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception e1) {
                throw new RuntimeException();
            }
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e2) {
                throw new RuntimeException();
            }
        }

        return result;
    }

    /**
     * Insert a single student in a battle.
     *
     * @param userId the student to join in the battle.
     * @param battleId the battle to join.
     * @return true if the student joins in the battle.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public boolean joinBattleAlone(int userId, int battleId) throws SQLException {
        //insert query
        String query = "INSERT INTO `new_schema`.`team` " +
                "(`numberStudent`, `battleId`, `phase`, `teamLeader`) " +
                "VALUES (?, ?, ?, ?)";
        //statement
        PreparedStatement preparedStatement = null;

        try{
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1,1);
            preparedStatement.setInt(2, battleId);
            preparedStatement.setString(3, TeamState.COMPLETE.getValue());
            preparedStatement.setInt(4, userId);
            preparedStatement.execute();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e2) {
                throw new RuntimeException();
            }
        }

        return true;
    }

    /**
     * Insert a team request in a battle.
     *
     * @param userId the student that creates the team.
     * @param battleId the battle to join.
     * @param teammateId the teammates.
     * @return true is the team is created correctly.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public boolean joinBattleAsTeam(int userId, int battleId, List<Integer> teammateId) throws SQLException {
        //insert query
        String queryTeam = "INSERT INTO `new_schema`.`team` " +
                "(`numberStudent`, `battleId`, `phase`, `teamLeader`, `points`) " +
                "VALUES (?, ?, ?, ?, ?)";
        String queryTeamStudent = "INSERT INTO `new_schema`.`team_student` " +
                "(`teamId`, `studentId`, `phase`) " +
                "VALUES (?, ?, ?)";
        //statement
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int teamId = -1;


        try {
            //start transaction
            con.setAutoCommit(false);

            preparedStatement = con.prepareStatement(queryTeam, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, teammateId.size());
            preparedStatement.setInt(2, battleId);
            preparedStatement.setString(3, TeamState.INCOMPLETE.getValue());
            preparedStatement.setInt(4, userId);
            preparedStatement.setInt(5, 0);
            preparedStatement.executeUpdate();

            resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.first()){
                teamId = resultSet.getInt(1);
            }
            //team is created
            if (teamId != -1) {
                for (Integer integer : teammateId) {
                    preparedStatement = con.prepareStatement(queryTeamStudent);
                    preparedStatement.setInt(1, teamId);
                    preparedStatement.setInt(2, integer);
                    preparedStatement.setString(3, TeamStudentState.ACCEPT.getValue());
                    preparedStatement.execute();
                }
            }
            else {
                new SQLException();
            }

            //end transaction
            con.commit();
        }
        catch (SQLException e){
            try {
                //transaction error ==> rollback
                con.rollback();
            }
            catch (SQLException e1){
                return false;
            }
            return false;
        }
        finally {
            try {
                if (preparedStatement != null){
                    preparedStatement.close();
                }
            }
            catch (SQLException e){
                throw new SQLException();
            }
        }

        return true;
    }

    /**
     * Check if student is in another team for the same battle.
     *
     * @param userId the student id.
     * @param battleId the battle id.
     * @return true if student is in another team for the same battle.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public boolean checkStudentInOtherTeam(int userId, int battleId) throws SQLException {
        //search query
        String query = "SELECT * " +
                "FROM team_student as ts, team as t " +
                "WHERE t.idteam = ts.teamId and ts.studentId = ? and ts.Phase = ? and t.battleId = ?";
        //statement
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean result = false;

        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, TeamStudentState.ACCEPT.getValue());
            preparedStatement.setInt(3, battleId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.first()){
                result = true;
            }
        }
        catch (SQLException e) {
            return false;
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e2) {
                throw new RuntimeException();
            }
        }

        return result;
    }

    /**
     * Database search for all student usernames not in a specific battle.
     *
     * @param userId the user id of the user making the request.
     * @param battleId the battle id to search.
     * @return list of all student requested.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<SessionUser> showStudentNotInBattle(int userId, int battleId) throws SQLException {
        //search query
        String query = "SELECT u.idUser, u.Username " +
                "FROM t_subscription as tsub, user as u, battle as b " +
                "WHERE tsub.UserId = u.idUser and tsub.TournamentId = b.TournamentId " +
                "   and b.idbattle = ? " +
                "   and not (u.idUser = ?) " +
                "   and u.Role = ? " +
                "   and tsub.UserId not in( " +
                "       SELECT ts.studentId " +
                "       FROM team_student as ts " +
                "       WHERE ts.phase = ?)";
        //statement
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ArrayList<SessionUser> result = new ArrayList<>();

        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, battleId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(3, UserRole.STUDENT.getValue());
            preparedStatement.setString(4, TeamStudentState.ACCEPT.getValue());
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                SessionUser sessionUser = new SessionUser();
                sessionUser.setId(resultSet.getInt("idUser"));
                sessionUser.setUsername(resultSet.getString("Username"));

                result.add(sessionUser);
            }
        }
        catch (SQLException e){
            return null;
        }
        finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (Exception e1) {
                throw new SQLException(e1);
            }
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (Exception e1) {
                throw new SQLException(e1);
            }
        }

        return result;
    }

    /**
     * Check if student has already creates a team.
     *
     * @param userId the user id of the request student.
     * @param battleId the battle id to search.
     * @return true if the student has already creates a team.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public boolean checkStudentHasCreatedATeam(int userId, int battleId) throws SQLException {
        //search query
        String query = "SELECT * " +
                "FROM team " +
                "WHERE teamLeader = ? and battleId = ?";
        //statement
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean result = false;

        try {
            preparedStatement = con.prepareStatement(query);
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, battleId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.first()) {
                result = true;
            }
        }
        catch (SQLException e) {
            result = false;
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e2) {
                throw new RuntimeException();
            }
        }

        return  result;
    }
}
