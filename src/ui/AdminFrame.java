/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ui;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import utils.DBConnection;

/**
 *
 * @author ADMIN
 */
public class AdminFrame extends javax.swing.JFrame {

    private Map<String, String> departmentMap = new HashMap<>();
    private Map<String, String> programMap = new HashMap<>();
    /**
     * Creates new form AdminFrame
     */
    public AdminFrame() {
        initComponents();
        
        HomeDashboard.setVisible(true);
        StudentInfo.setVisible(false);
        Curriculum.setVisible(false);
        SubjectCourses.setVisible(false);
        Programs.setVisible(false);
        StudentList.setVisible(false);
        StudentEnrollment.setVisible(false);
        
        loadCourses("Department", "Program", "");
        loadDepartments();
        loadPrograms("Department");
        loadDeptartmentP();
    }
    
    private void loadDeptartmentP(){
        String sql = "SELECT college_name FROM colleges"; // Adjust the SQL query as needed
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Department");
            while (rs.next()) {
                String collegeName = rs.getString("college_name");
                String abbreviation = formatAbbreviation(collegeName);
                model.addElement(abbreviation);
                departmentMap.put(abbreviation, collegeName);
            }
            cbDepartmentP.setModel(model);
            cbDepartmentP.setSelectedItem("Department");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void loadPrograms(String department){
        DefaultTableModel model = (DefaultTableModel) tbProgram.getModel();
        model.setRowCount(0); // Clear existing rows

        String sql = "SELECT p.program_name FROM programs p INNER JOIN program_college pc ON p.program_id = pc.program_id INNER JOIN colleges c ON pc.college_id = c.college_id"; // SQL query to retrieve program names
        boolean hasDepartment = !department.equals("Department");
        if (hasDepartment) {
            sql += " WHERE c.college_name = ?";
        }
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ptst = conn.prepareStatement(sql);
            if (hasDepartment) {
                ptst.setString(1, departmentMap.get(department));
            }
            ResultSet rs = ptst.executeQuery();

            // Iterate through the result set and add each program name to the table model
            while (rs.next()) {
                String programName = rs.getString("program_name");
                model.addRow(new Object[]{programName}); // Add the program name as a new row
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void loadCourses(String department, String program, String searchTerm) {
        DefaultTableModel model = (DefaultTableModel) tbCourses.getModel();
        model.setRowCount(0);

        String sql = """
             SELECT c.course_code, c.course_title, c.units, c.lec_hrs, c.lab_hrs, c.year_level, c.semester
             FROM courses c
             INNER JOIN course_program cp ON c.course_id = cp.course_id
             INNER JOIN programs p ON cp.program_id = p.program_id
             INNER JOIN program_college pc ON p.program_id = pc.program_id
             INNER JOIN colleges d ON pc.college_id = d.college_id
             """;

        StringBuilder whereClause = new StringBuilder();
        boolean hasDepartmentFilter = !department.equals("Department");
        boolean hasProgramFilter = program != null && !program.equals("Program");
        boolean hasSearchTerm = !searchTerm.isEmpty();

        if (hasDepartmentFilter || hasProgramFilter || hasSearchTerm) {
            whereClause.append("WHERE ");
            if (hasDepartmentFilter) {
                whereClause.append("d.college_name = ? ");
            }
            if (hasProgramFilter) {
                if (hasDepartmentFilter) {
                    whereClause.append("AND ");
                }
                whereClause.append("p.program_name = ? ");
            }
            if (hasSearchTerm) {
                if (hasDepartmentFilter || hasProgramFilter) {
                    whereClause.append("AND ");
                }
                whereClause.append("(c.course_code LIKE ? OR c.course_title LIKE ?) ");
            }
        }

        // Combine the base SQL with the WHERE clause
        sql += whereClause.toString();

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int paramIndex = 1;

                if (hasDepartmentFilter) {
                    ps.setString(paramIndex++, department); // Get full name from map
                }
                if (hasProgramFilter) {
                    ps.setString(paramIndex++, program); // Get full name from map
                }
                if (hasSearchTerm) {
                    ps.setString(paramIndex++, "%" + searchTerm + "%"); // Search in course_code
                    ps.setString(paramIndex++, "%" + searchTerm + "%"); // Search in course_title
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String courseCode = rs.getString("course_code");
                        String courseTitle = rs.getString("course_title");
                        int units = rs.getInt("units");
                        int lecHrs = rs.getInt("lec_hrs");
                        int labHrs = rs.getInt("lab_hrs");
                        int yearLevel = rs.getInt("year_level");
                        int semester = rs.getInt("semester");

                        String formattedYearLevel = getOrdinalSuffix(yearLevel);
                        String formattedSemester = getOrdinalSuffix(semester);
                        String lec = lecHrs + " hours", lab = labHrs + " hours";

                        if (lecHrs == 1 && lecHrs != 0) {
                            lec = lecHrs + " hour";
                        }

                        if (labHrs == 1 && labHrs != 0) {
                            lab = labHrs + " hour";
                        }
                        model.addRow(new Object[]{courseCode, courseTitle, units, lec, lab, formattedYearLevel, formattedSemester});
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String getOrdinalSuffix(int number) {
        if (number <= 0) {
            return String.valueOf(number); // Handle non-positive numbers
        }
        String suffix;
        switch (number % 10) {
            case 1:
                suffix = (number % 100 == 11) ? "th" : "st"; // 11th, 21st, etc.
                break;
            case 2:
                suffix = (number % 100 == 12) ? "th" : "nd"; // 12th, 22nd, etc.
                break;
            case 3:
                suffix = (number % 100 == 13) ? "th" : "rd"; // 13th, 23rd, etc.
                break;
            default:
                suffix = "th"; // All other cases
                break;
        }
        return number + suffix; // Return the number with its suffix
    }
    
    private void loadDepartments() {
        String sql = "SELECT college_name FROM colleges"; // Adjust the SQL query as needed
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Department");
            while (rs.next()) {
                String collegeName = rs.getString("college_name");
                String abbreviation = formatAbbreviation(collegeName);
                model.addElement(abbreviation);
                departmentMap.put(abbreviation, collegeName);
            }
            cbDepartmentS.setModel(model);
            cbDepartmentS.setSelectedItem("Department");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProgramsCb(String department) {
        String sql = "SELECT p.program_name FROM programs p INNER JOIN program_college pc ON p.program_id = pc.program_id INNER JOIN colleges c ON pc.college_id = c.college_id WHERE c.college_name = ?"; // Adjust the SQL query as needed
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, departmentMap.get(department)); // Get full name from map
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Program");
            while (rs.next()) {
                String programName = rs.getString("program_name");
                String abbreviation = formatAbbreviation(programName);
                model.addElement(abbreviation);
                programMap.put(abbreviation, programName);
            }
            cbProgramS.setModel(model);
            cbProgramS.setSelectedItem("Program");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatAbbreviation(String name) {
        String[] words = name.split(" ");
        StringBuilder abbreviation = new StringBuilder();
        for (String word : words) {
            // Exclude short words
            if (word.length() > 2 && !word.equalsIgnoreCase("of") && !word.equalsIgnoreCase("in") && !word.equalsIgnoreCase("and")) {
                abbreviation.append(word.charAt(0));
            }
        }
        return abbreviation.toString().toUpperCase(); // Return the abbreviation in uppercase
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        Header = new javax.swing.JPanel();
        lblDate = new javax.swing.JLabel();
        lblTime = new javax.swing.JLabel();
        lblHome = new javax.swing.JLabel();
        NavBar = new javax.swing.JPanel();
        lblStudentInfo = new javax.swing.JLabel();
        lblCurriculum = new javax.swing.JLabel();
        lblSubjectCourses = new javax.swing.JLabel();
        lblPrograms = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Content = new javax.swing.JLayeredPane();
        Curriculum = new javax.swing.JPanel();
        StudentInfo = new javax.swing.JPanel();
        lblStudentList = new javax.swing.JLabel();
        lblUploadGrade = new javax.swing.JLabel();
        lblEvaluation = new javax.swing.JLabel();
        StudentPanel = new javax.swing.JLayeredPane();
        StudentList = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        TbStudents = new javax.swing.JTable();
        TxtStudentSearch = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton4 = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox<>();
        jComboBox3 = new javax.swing.JComboBox<>();
        BtnEnroll = new javax.swing.JButton();
        StudentEnrollment = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel10 = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        TxtMiddleName = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        TxtLastName = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        TxtFirstName = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        BxGender = new javax.swing.JComboBox<>();
        jLabel31 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        TxtEmail = new javax.swing.JTextField();
        jLabel33 = new javax.swing.JLabel();
        TxtContactNumber = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        RBregular = new javax.swing.JRadioButton();
        RBirregular = new javax.swing.JRadioButton();
        RBtransferee = new javax.swing.JRadioButton();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        BxProgram = new javax.swing.JComboBox<>();
        BxYearLevel = new javax.swing.JComboBox<>();
        jLabel38 = new javax.swing.JLabel();
        BxCurriculum = new javax.swing.JComboBox<>();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        SubjectCourses = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tbCourses = new javax.swing.JTable();
        txtSearchCourse = new javax.swing.JTextField();
        lblSearchCourse = new javax.swing.JLabel();
        cbProgramS = new javax.swing.JComboBox<>();
        cbDepartmentS = new javax.swing.JComboBox<>();
        btnFilterCourses = new javax.swing.JButton();
        Programs = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tbProgram = new javax.swing.JTable();
        txtSearchProgram = new javax.swing.JTextField();
        lblSearchProgram = new javax.swing.JLabel();
        cbDepartmentP = new javax.swing.JComboBox<>();
        HomeDashboard = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jPanel1.setPreferredSize(new java.awt.Dimension(1000, 700));

        Header.setBackground(new java.awt.Color(255, 255, 255));
        Header.setPreferredSize(new java.awt.Dimension(1000, 110));

        lblDate.setFont(new java.awt.Font("Open Sans", 0, 18)); // NOI18N
        lblDate.setForeground(new java.awt.Color(0, 0, 0));
        lblDate.setText("Date!");

        lblTime.setFont(new java.awt.Font("Open Sans", 0, 18)); // NOI18N
        lblTime.setForeground(new java.awt.Color(0, 0, 0));
        lblTime.setText("Time!");

        lblHome.setFont(new java.awt.Font("Montserrat", 1, 24)); // NOI18N
        lblHome.setForeground(new java.awt.Color(0, 0, 0));
        lblHome.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/BSU-LOGO-60x60.png"))); // NOI18N
        lblHome.setText("  Batangas State University");
        lblHome.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblHome.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblHomeMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout HeaderLayout = new javax.swing.GroupLayout(Header);
        Header.setLayout(HeaderLayout);
        HeaderLayout.setHorizontalGroup(
            HeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HeaderLayout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(lblHome)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(HeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(HeaderLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(lblDate))
                    .addComponent(lblTime))
                .addGap(34, 34, 34))
        );
        HeaderLayout.setVerticalGroup(
            HeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HeaderLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblDate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTime)
                .addContainerGap(25, Short.MAX_VALUE))
            .addComponent(lblHome, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        NavBar.setBackground(new java.awt.Color(220, 220, 220));

        lblStudentInfo.setFont(new java.awt.Font("Open Sans", 0, 14)); // NOI18N
        lblStudentInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblStudentInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/group-solid-48.png"))); // NOI18N
        lblStudentInfo.setText(" Student Information");
        lblStudentInfo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblStudentInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblStudentInfoMouseClicked(evt);
            }
        });

        lblCurriculum.setFont(new java.awt.Font("Open Sans", 0, 14)); // NOI18N
        lblCurriculum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCurriculum.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/file-solid-36.png"))); // NOI18N
        lblCurriculum.setText("  Curriculum");
        lblCurriculum.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblCurriculum.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblCurriculumMouseClicked(evt);
            }
        });

        lblSubjectCourses.setFont(new java.awt.Font("Open Sans", 0, 14)); // NOI18N
        lblSubjectCourses.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSubjectCourses.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/book-solid-36.png"))); // NOI18N
        lblSubjectCourses.setText("   Subject/Courses");
        lblSubjectCourses.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblSubjectCourses.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblSubjectCoursesMouseClicked(evt);
            }
        });

        lblPrograms.setFont(new java.awt.Font("Open Sans", 0, 14)); // NOI18N
        lblPrograms.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPrograms.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/bank-solid-36.png"))); // NOI18N
        lblPrograms.setText("  Programs");
        lblPrograms.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblPrograms.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblProgramsMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout NavBarLayout = new javax.swing.GroupLayout(NavBar);
        NavBar.setLayout(NavBarLayout);
        NavBarLayout.setHorizontalGroup(
            NavBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(NavBarLayout.createSequentialGroup()
                .addGap(70, 70, 70)
                .addComponent(lblStudentInfo)
                .addGap(60, 60, 60)
                .addComponent(lblCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(96, 96, 96)
                .addComponent(lblSubjectCourses)
                .addGap(76, 76, 76)
                .addComponent(lblPrograms, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        NavBarLayout.setVerticalGroup(
            NavBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(NavBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(lblSubjectCourses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblPrograms, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(lblCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(lblStudentInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout CurriculumLayout = new javax.swing.GroupLayout(Curriculum);
        Curriculum.setLayout(CurriculumLayout);
        CurriculumLayout.setHorizontalGroup(
            CurriculumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 994, Short.MAX_VALUE)
        );
        CurriculumLayout.setVerticalGroup(
            CurriculumLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 484, Short.MAX_VALUE)
        );

        StudentInfo.setBackground(new java.awt.Color(247, 247, 247));

        lblStudentList.setFont(new java.awt.Font("Open Sans", 0, 18)); // NOI18N
        lblStudentList.setText("Student List");
        lblStudentList.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblStudentList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblStudentListMouseClicked(evt);
            }
        });

        lblUploadGrade.setFont(new java.awt.Font("Open Sans", 0, 18)); // NOI18N
        lblUploadGrade.setText("Upload Grade");

        lblEvaluation.setFont(new java.awt.Font("Open Sans", 0, 18)); // NOI18N
        lblEvaluation.setText("Evaluation");

        StudentPanel.setPreferredSize(new java.awt.Dimension(982, 374));

        StudentList.setBackground(new java.awt.Color(247, 247, 247));

        jLabel22.setFont(new java.awt.Font("Open Sans", 1, 22)); // NOI18N
        jLabel22.setText("List Of Students");

        jPanel9.setBackground(new java.awt.Color(247, 247, 247));
        jPanel9.setLayout(new java.awt.BorderLayout());

        TbStudents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "SR-Code", "Name", "Program", "Type", "Sex", "Year Level"
            }
        ));
        jScrollPane4.setViewportView(TbStudents);

        jPanel9.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jLabel23.setText("Search:");

        jButton2.setText("Delete Info");

        jButton3.setText("Edit Info");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Regular", "Irregular" }));

        jButton4.setText("Filter");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "INC", "DRP" }));

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "w/Failed Grades", "w/out Failed Grades" }));

        BtnEnroll.setText("Enroll Student");
        BtnEnroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnEnrollActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout StudentListLayout = new javax.swing.GroupLayout(StudentList);
        StudentList.setLayout(StudentListLayout);
        StudentListLayout.setHorizontalGroup(
            StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentListLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(StudentListLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 140, Short.MAX_VALUE)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TxtStudentSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StudentListLayout.createSequentialGroup()
                        .addComponent(BtnEnroll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        StudentListLayout.setVerticalGroup(
            StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentListLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton4)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(TxtStudentSearch)
                    .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(StudentListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(BtnEnroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        StudentEnrollment.setBackground(new java.awt.Color(247, 247, 247));

        jLabel24.setFont(new java.awt.Font("Open Sans", 1, 18)); // NOI18N
        jLabel24.setText("Student Registration and Enrollment");

        jScrollPane5.setPreferredSize(new java.awt.Dimension(940, 600));

        jPanel10.setPreferredSize(new java.awt.Dimension(928, 460));

        jLabel25.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel25.setText("Student Information");

        jLabel27.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel27.setText("Middle Name");

        TxtMiddleName.setPreferredSize(new java.awt.Dimension(68, 31));

        jLabel28.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel28.setText("Last Name");

        TxtLastName.setPreferredSize(new java.awt.Dimension(68, 31));

        jLabel29.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel29.setText("First Name");

        TxtFirstName.setPreferredSize(new java.awt.Dimension(68, 31));

        jLabel30.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel30.setText("Gender");

        BxGender.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        BxGender.setPreferredSize(new java.awt.Dimension(76, 31));

        jLabel31.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel31.setText("Date Of Birth");

        jLabel32.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel32.setText("Email Address");

        TxtEmail.setPreferredSize(new java.awt.Dimension(68, 31));

        jLabel33.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel33.setText("Contact Number");

        TxtContactNumber.setPreferredSize(new java.awt.Dimension(68, 31));

        jLabel34.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel34.setText("Student Type");

        RBregular.setText("Regular");

        RBirregular.setText("Irregular");

        RBtransferee.setText("Transferee");

        jLabel35.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel35.setText("Program Information");

        jLabel36.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel36.setText("Program");

        jLabel37.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel37.setText("Year Level");

        BxProgram.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        BxYearLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel38.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel38.setText("Curriculum");

        BxCurriculum.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButton6.setText("Cancel");
        jButton6.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jButton7.setBackground(new java.awt.Color(51, 51, 51));
        jButton7.setForeground(new java.awt.Color(255, 255, 255));
        jButton7.setText("Register Student");
        jButton7.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel25))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel29)
                        .addGap(198, 198, 198)
                        .addComponent(jLabel27)
                        .addGap(184, 184, 184)
                        .addComponent(jLabel28))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(TxtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(TxtMiddleName, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(21, 21, 21)
                        .addComponent(TxtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel35))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel36)
                        .addGap(210, 210, 210)
                        .addComponent(jLabel38))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(BxProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BxCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel37))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel31)
                            .addComponent(jLabel32)
                            .addComponent(TxtEmail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel10Layout.createSequentialGroup()
                                .addComponent(jDateChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                                .addGap(6, 6, 6)))
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel10Layout.createSequentialGroup()
                                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel10Layout.createSequentialGroup()
                                        .addGap(15, 15, 15)
                                        .addComponent(jLabel33))
                                    .addGroup(jPanel10Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(TxtContactNumber, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(21, 21, 21)
                                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel30)
                                    .addComponent(BxGender, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel34)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                                        .addGap(4, 4, 4)
                                        .addComponent(RBregular)
                                        .addGap(12, 12, 12)
                                        .addComponent(RBtransferee)
                                        .addGap(12, 12, 12)
                                        .addComponent(RBirregular)))
                                .addGap(258, 258, 258)))))
                .addContainerGap(151, Short.MAX_VALUE))
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(BxYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton6)
                .addGap(6, 6, 6)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel25)
                .addGap(12, 12, 12)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel29)
                    .addComponent(jLabel27)
                    .addComponent(jLabel28))
                .addGap(6, 6, 6)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TxtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(TxtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(TxtMiddleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel30)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addGap(6, 6, 6)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(TxtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(BxGender, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(TxtContactNumber, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel33))
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addGap(11, 11, 11)
                        .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addGap(11, 11, 11)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(RBregular)
                            .addComponent(RBtransferee)
                            .addComponent(RBirregular))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel35)
                .addGap(18, 18, 18)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jLabel36))
                    .addComponent(jLabel38))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(BxProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BxCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel37)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel10Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BxYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(15, 15, 15))))
        );

        jScrollPane5.setViewportView(jPanel10);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 958, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout StudentEnrollmentLayout = new javax.swing.GroupLayout(StudentEnrollment);
        StudentEnrollment.setLayout(StudentEnrollmentLayout);
        StudentEnrollmentLayout.setHorizontalGroup(
            StudentEnrollmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentEnrollmentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StudentEnrollmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(StudentEnrollmentLayout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        StudentEnrollmentLayout.setVerticalGroup(
            StudentEnrollmentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentEnrollmentLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        StudentPanel.setLayer(StudentList, javax.swing.JLayeredPane.DEFAULT_LAYER);
        StudentPanel.setLayer(StudentEnrollment, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout StudentPanelLayout = new javax.swing.GroupLayout(StudentPanel);
        StudentPanel.setLayout(StudentPanelLayout);
        StudentPanelLayout.setHorizontalGroup(
            StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 973, Short.MAX_VALUE)
            .addGroup(StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(StudentPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(StudentList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addGroup(StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, StudentPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(StudentEnrollment, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        StudentPanelLayout.setVerticalGroup(
            StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 372, Short.MAX_VALUE)
            .addGroup(StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(StudentPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(StudentList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addGroup(StudentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(StudentPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(StudentEnrollment, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout StudentInfoLayout = new javax.swing.GroupLayout(StudentInfo);
        StudentInfo.setLayout(StudentInfoLayout);
        StudentInfoLayout.setHorizontalGroup(
            StudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(StudentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 973, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(StudentInfoLayout.createSequentialGroup()
                        .addComponent(lblStudentList)
                        .addGap(33, 33, 33)
                        .addComponent(lblUploadGrade)
                        .addGap(33, 33, 33)
                        .addComponent(lblEvaluation)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        StudentInfoLayout.setVerticalGroup(
            StudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(StudentInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(StudentInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStudentList)
                    .addComponent(lblUploadGrade)
                    .addComponent(lblEvaluation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(StudentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 372, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        SubjectCourses.setBackground(new java.awt.Color(247, 247, 247));

        jLabel1.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel1.setText("List of Courses");

        tbCourses.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        tbCourses.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Code", "Title", "Units", "Lec Hrs", "Lab Hrs", "Year Level", "Semester"
            }
        ));
        tbCourses.setRowHeight(30);
        tbCourses.getTableHeader().setResizingAllowed(false);
        tbCourses.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tbCourses);
        if (tbCourses.getColumnModel().getColumnCount() > 0) {
            tbCourses.getColumnModel().getColumn(0).setMinWidth(70);
            tbCourses.getColumnModel().getColumn(0).setMaxWidth(70);
            tbCourses.getColumnModel().getColumn(2).setMinWidth(50);
            tbCourses.getColumnModel().getColumn(2).setMaxWidth(50);
            tbCourses.getColumnModel().getColumn(3).setMinWidth(70);
            tbCourses.getColumnModel().getColumn(3).setMaxWidth(70);
            tbCourses.getColumnModel().getColumn(4).setMinWidth(70);
            tbCourses.getColumnModel().getColumn(4).setMaxWidth(70);
            tbCourses.getColumnModel().getColumn(5).setMinWidth(120);
            tbCourses.getColumnModel().getColumn(5).setMaxWidth(120);
            tbCourses.getColumnModel().getColumn(6).setMinWidth(120);
            tbCourses.getColumnModel().getColumn(6).setMaxWidth(120);
        }

        txtSearchCourse.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        txtSearchCourse.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchCourseKeyPressed(evt);
            }
        });

        lblSearchCourse.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSearchCourse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/search-regular-24.png"))); // NOI18N

        cbProgramS.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Program" }));

        cbDepartmentS.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Department" }));
        cbDepartmentS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbDepartmentSActionPerformed(evt);
            }
        });

        btnFilterCourses.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        btnFilterCourses.setText("Filter");
        btnFilterCourses.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterCoursesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout SubjectCoursesLayout = new javax.swing.GroupLayout(SubjectCourses);
        SubjectCourses.setLayout(SubjectCoursesLayout);
        SubjectCoursesLayout.setHorizontalGroup(
            SubjectCoursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SubjectCoursesLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(SubjectCoursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(SubjectCoursesLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(119, 119, 119)
                        .addComponent(cbDepartmentS, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(cbProgramS, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnFilterCourses)
                        .addGap(30, 30, 30)
                        .addComponent(lblSearchCourse)
                        .addGap(6, 6, 6)
                        .addComponent(txtSearchCourse, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 956, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        SubjectCoursesLayout.setVerticalGroup(
            SubjectCoursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SubjectCoursesLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(SubjectCoursesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(cbDepartmentS, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbProgramS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSearchCourse, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSearchCourse, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFilterCourses, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );

        Programs.setBackground(new java.awt.Color(247, 247, 247));

        jLabel4.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel4.setText("List of Programs");

        tbProgram.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Program"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tbProgram.setRowHeight(30);
        tbProgram.getTableHeader().setResizingAllowed(false);
        tbProgram.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tbProgram);
        if (tbProgram.getColumnModel().getColumnCount() > 0) {
            tbProgram.getColumnModel().getColumn(0).setResizable(false);
        }

        txtSearchProgram.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N

        lblSearchProgram.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSearchProgram.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/search-regular-24.png"))); // NOI18N

        cbDepartmentP.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Department" }));
        cbDepartmentP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbDepartmentPActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ProgramsLayout = new javax.swing.GroupLayout(Programs);
        Programs.setLayout(ProgramsLayout);
        ProgramsLayout.setHorizontalGroup(
            ProgramsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProgramsLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(ProgramsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ProgramsLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(362, 362, 362)
                        .addComponent(cbDepartmentP, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblSearchProgram)
                        .addGap(6, 6, 6)
                        .addComponent(txtSearchProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 944, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ProgramsLayout.setVerticalGroup(
            ProgramsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProgramsLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(ProgramsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(cbDepartmentP, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSearchProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSearchProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(19, Short.MAX_VALUE))
        );

        HomeDashboard.setBackground(new java.awt.Color(247, 247, 247));

        jLabel8.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel8.setText("Dashboard");

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/bank-solid-84.png"))); // NOI18N

        jLabel10.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel10.setText("Total Programs");

        jLabel11.setFont(new java.awt.Font("Montserrat ExtraBold", 0, 24)); // NOI18N
        jLabel11.setText("0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(31, 31, 31))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/book-solid-84.png"))); // NOI18N

        jLabel13.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel13.setText("Courses");

        jLabel14.setFont(new java.awt.Font("Montserrat ExtraBold", 0, 24)); // NOI18N
        jLabel14.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(10, Short.MAX_VALUE)
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/group-solid-84.png"))); // NOI18N

        jLabel16.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel16.setText("Total Students");

        jLabel17.setFont(new java.awt.Font("Montserrat ExtraBold", 0, 24)); // NOI18N
        jLabel17.setText("0");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(37, 37, 37))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/buildings-solid-84.png"))); // NOI18N

        jLabel19.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jLabel19.setText("Departments");

        jLabel20.setFont(new java.awt.Font("Montserrat ExtraBold", 0, 24)); // NOI18N
        jLabel20.setText("0");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(37, 37, 37))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel7.setBackground(new java.awt.Color(230, 230, 230));

        jLabel21.setFont(new java.awt.Font("Open Sans", 1, 14)); // NOI18N
        jLabel21.setText("Quick Actions");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel21)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel21)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jButton1.setFont(new java.awt.Font("Open Sans", 0, 12)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imgs/user-plus-solid-24.png"))); // NOI18N
        jButton1.setText("  Enroll Student");
        jButton1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 196, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 11, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout HomeDashboardLayout = new javax.swing.GroupLayout(HomeDashboard);
        HomeDashboard.setLayout(HomeDashboardLayout);
        HomeDashboardLayout.setHorizontalGroup(
            HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HomeDashboardLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addGroup(HomeDashboardLayout.createSequentialGroup()
                        .addGroup(HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        HomeDashboardLayout.setVerticalGroup(
            HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(HomeDashboardLayout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addComponent(jLabel8)
                .addGap(16, 16, 16)
                .addGroup(HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(HomeDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(203, Short.MAX_VALUE))
        );

        Content.setLayer(Curriculum, javax.swing.JLayeredPane.DEFAULT_LAYER);
        Content.setLayer(StudentInfo, javax.swing.JLayeredPane.DEFAULT_LAYER);
        Content.setLayer(SubjectCourses, javax.swing.JLayeredPane.DEFAULT_LAYER);
        Content.setLayer(Programs, javax.swing.JLayeredPane.DEFAULT_LAYER);
        Content.setLayer(HomeDashboard, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout ContentLayout = new javax.swing.GroupLayout(Content);
        Content.setLayout(ContentLayout);
        ContentLayout.setHorizontalGroup(
            ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(StudentInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(Curriculum, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(SubjectCourses, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(Programs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(HomeDashboard, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ContentLayout.setVerticalGroup(
            ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(StudentInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(Curriculum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(SubjectCourses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(Programs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(HomeDashboard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(Content);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Header, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(NavBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(Header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(NavBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1000, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void lblStudentInfoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblStudentInfoMouseClicked
        HomeDashboard.setVisible(false);
        StudentInfo.setVisible(true);
        Curriculum.setVisible(false);
        SubjectCourses.setVisible(false);
        Programs.setVisible(false);
        StudentList.setVisible(true);
        
        Content.revalidate();
        Content.repaint();
    }//GEN-LAST:event_lblStudentInfoMouseClicked

    private void lblCurriculumMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblCurriculumMouseClicked
        HomeDashboard.setVisible(false);
        StudentInfo.setVisible(false);
        Curriculum.setVisible(true);
        SubjectCourses.setVisible(false);
        Programs.setVisible(false);
        
        Content.revalidate();
        Content.repaint();
    }//GEN-LAST:event_lblCurriculumMouseClicked

    private void lblSubjectCoursesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblSubjectCoursesMouseClicked
        HomeDashboard.setVisible(false);
        StudentInfo.setVisible(false);
        Curriculum.setVisible(false);
        SubjectCourses.setVisible(true);
        Programs.setVisible(false);
        
        Content.revalidate();
        Content.repaint();
    }//GEN-LAST:event_lblSubjectCoursesMouseClicked

    private void lblProgramsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblProgramsMouseClicked
        HomeDashboard.setVisible(false);
        StudentInfo.setVisible(false);
        Curriculum.setVisible(false);
        SubjectCourses.setVisible(false);
        Programs.setVisible(true);
        
        Content.revalidate();
        Content.repaint();
    }//GEN-LAST:event_lblProgramsMouseClicked

    private void lblHomeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblHomeMouseClicked
        HomeDashboard.setVisible(true);
        StudentInfo.setVisible(false);
        Curriculum.setVisible(false);
        SubjectCourses.setVisible(false);
        Programs.setVisible(false);
        
        Content.revalidate();
        Content.repaint();
    }//GEN-LAST:event_lblHomeMouseClicked

    private void lblStudentListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblStudentListMouseClicked
        StudentList.setVisible(true);
        StudentEnrollment.setVisible(false);

        StudentPanel.revalidate();
        StudentPanel.repaint();
    }//GEN-LAST:event_lblStudentListMouseClicked

    private void BtnEnrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnEnrollActionPerformed
        StudentEnrollment.setVisible(true);
        StudentList.setVisible(false);

        StudentPanel.revalidate();
        StudentPanel.repaint();
    }//GEN-LAST:event_BtnEnrollActionPerformed

    private void btnFilterCoursesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterCoursesActionPerformed
        String departmentAbbreviation = (String) cbDepartmentS.getSelectedItem();
        String programAbbreviation = (String) cbProgramS.getSelectedItem();
        String department = departmentAbbreviation, program = programAbbreviation;

        // Check for null values
        if (departmentAbbreviation == null || programAbbreviation == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid department and program.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!departmentAbbreviation.equals("Department")) {
            // Get the full names from the maps
            department = departmentMap.get(departmentAbbreviation);
            program = programMap.get(programAbbreviation);
        }

        // If the program is "Program", set it to null to load all courses in the department
        if (programAbbreviation.equals("Program")) {
            program = null; // This will allow loading all courses in the selected department
        }

        loadCourses(department, program, "");
    }//GEN-LAST:event_btnFilterCoursesActionPerformed

    private void cbDepartmentSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbDepartmentSActionPerformed
        String selectedDepartment = (String) cbDepartmentS.getSelectedItem();
        if (selectedDepartment != null && !selectedDepartment.equals("Department")) {
            loadProgramsCb(selectedDepartment); // Load programs based on selected department
        } else {
            cbProgramS.setModel(new DefaultComboBoxModel<>(new String[]{"Program"})); // Reset programs
        }
    }//GEN-LAST:event_cbDepartmentSActionPerformed

    private void txtSearchCourseKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchCourseKeyPressed
        String search = txtSearchCourse.getText();
        loadCourses("Department", "Program", search);
    }//GEN-LAST:event_txtSearchCourseKeyPressed

    private void cbDepartmentPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbDepartmentPActionPerformed
        String department = (String) cbDepartmentP.getSelectedItem();
        loadPrograms(department);
    }//GEN-LAST:event_cbDepartmentPActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnEnroll;
    private javax.swing.JComboBox<String> BxCurriculum;
    private javax.swing.JComboBox<String> BxGender;
    private javax.swing.JComboBox<String> BxProgram;
    private javax.swing.JComboBox<String> BxYearLevel;
    private javax.swing.JLayeredPane Content;
    private javax.swing.JPanel Curriculum;
    private javax.swing.JPanel Header;
    private javax.swing.JPanel HomeDashboard;
    private javax.swing.JPanel NavBar;
    private javax.swing.JPanel Programs;
    private javax.swing.JRadioButton RBirregular;
    private javax.swing.JRadioButton RBregular;
    private javax.swing.JRadioButton RBtransferee;
    private javax.swing.JPanel StudentEnrollment;
    private javax.swing.JPanel StudentInfo;
    private javax.swing.JPanel StudentList;
    private javax.swing.JLayeredPane StudentPanel;
    private javax.swing.JPanel SubjectCourses;
    private javax.swing.JTable TbStudents;
    private javax.swing.JTextField TxtContactNumber;
    private javax.swing.JTextField TxtEmail;
    private javax.swing.JTextField TxtFirstName;
    private javax.swing.JTextField TxtLastName;
    private javax.swing.JTextField TxtMiddleName;
    private javax.swing.JTextField TxtStudentSearch;
    private javax.swing.JButton btnFilterCourses;
    private javax.swing.JComboBox<String> cbDepartmentP;
    private javax.swing.JComboBox<String> cbDepartmentS;
    private javax.swing.JComboBox<String> cbProgramS;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JLabel lblCurriculum;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblEvaluation;
    private javax.swing.JLabel lblHome;
    private javax.swing.JLabel lblPrograms;
    private javax.swing.JLabel lblSearchCourse;
    private javax.swing.JLabel lblSearchProgram;
    private javax.swing.JLabel lblStudentInfo;
    private javax.swing.JLabel lblStudentList;
    private javax.swing.JLabel lblSubjectCourses;
    private javax.swing.JLabel lblTime;
    private javax.swing.JLabel lblUploadGrade;
    private javax.swing.JTable tbCourses;
    private javax.swing.JTable tbProgram;
    private javax.swing.JTextField txtSearchCourse;
    private javax.swing.JTextField txtSearchProgram;
    // End of variables declaration//GEN-END:variables
}
