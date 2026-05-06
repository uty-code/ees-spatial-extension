
$connString = 'Server=tcp:ees-server-hk.database.windows.net,1433;Initial Catalog=ees-db-hk;Persist Security Info=False;User ID=dbmaster;Password=EesEval2026;MultipleActiveResultSets=False;Encrypt=True;TrustServerCertificate=False;Connection Timeout=30;'
$connection = New-Object System.Data.SqlClient.SqlConnection($connString)
$query = "
    SET STATISTICS IO ON;
    SET STATISTICS TIME ON;
    -- 가장 무거운 매핑 조회 쿼리 테스트
    SELECT 
        m.mapping_id, e1.name as EVALUATEE_NAME, e2.name as EVALUATOR_NAME, d.dept_name
    FROM dbo.evaluator_mappings_51 m
    INNER JOIN dbo.employees_51 e1 ON m.evaluatee_id = e1.emp_id
    INNER JOIN dbo.employees_51 e2 ON m.evaluator_id = e2.emp_id
    LEFT JOIN dbo.departments_51 d ON e1.dept_id = d.dept_id;
"
$command = New-Object System.Data.SqlClient.SqlCommand($query, $connection)
$connection.Open()
$stats = New-Object System.Collections.Generic.List[string]
$connection.add_InfoMessage({
    param($sender, $event)
    $stats.Add($event.Message)
})
$adapter = New-Object System.Data.SqlClient.SqlDataAdapter($command)
$dataset = New-Object System.Data.DataSet
$adapter.Fill($dataset) | Out-Null
$connection.Close()
$stats | Out-File -FilePath 'before_stats.txt'
Write-Host "Performance stats saved to before_stats.txt"
