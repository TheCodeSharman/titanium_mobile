<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="content-type" content="text/html; charset=utf-8"/>
		<title>Reporting</title>

		<script type="text/javascript">
			function updateSummary(totalPassed, totalFailed) {
				document.getElementById("totalPassed").innerHTML = totalPassed;
				document.getElementById("totalFailed").innerHTML = totalFailed;
			}
		</script>
	</head>
	<body>
		<div>
			<h2 style="text-align: left; margin-bottom: 30px">Detailed results for run ID [<?php echo $_GET["runId"]; ?>] and driver ID [<?php echo $_GET["driverId"]; ?>]</h2>
		</div>

		<div style="margin-left: 50px;">
			Summary

			<div style="margin-left: 20px">Total Pass: <div id="totalPassed" style="display: inline"></div></div>
			<div style="margin-left: 20px">Total Fail: <div id="totalFailed" style="display: inline"></div></div>
		</div>

		<div style="margin-top: 50px">
<?php
	$totalPassed = 0;
	$totalFailed = 0;

	require "common.php";
	db_open();

	echo "<!--  START GENERATED -->\n";

	$query="SELECT * FROM driver_runs WHERE run_id = " . $_GET["run_id"] . 
		" AND driver_id = \"" . $_GET["driver_id"] . "\"";

	$result=mysql_query($query);
	while($row = mysql_fetch_array($result)) {
		$query2="SELECT * FROM config_sets WHERE driver_run_id = " . $row["id"];
		$result2=mysql_query($query2);
		while($row2 = mysql_fetch_array($result2)) {
			echo "<div style=\"margin-left: 50px;\">\n";
			echo "\t<div>Config Set: " . $row2["name"] . "</div>\n\n";

			$query3="SELECT * FROM configs WHERE config_set_id = " . $row2["id"];
			$result3=mysql_query($query3);
			while($row3 = mysql_fetch_array($result3)) {
				echo "\t<div style=\"margin-left: 50px;\">\n";
				echo "\t\t<div>Config: " . $row3["name"] . "</div>\n\n";

				$query4="SELECT * FROM suites WHERE config_id = " . $row3["id"];
				$result4=mysql_query($query4);
				while($row4 = mysql_fetch_array($result4)) {
					echo "\t\t<div style=\"margin-left: 50px; margin-bottom: 50px\">\n";
					echo "\t\t\t<div>Suite: " . $row4["name"] . "</div>\n\n";

					echo "\t\t\t<table border=\"1\">\n";
					echo "\t\t\t\t<tr>\n";
					echo "\t\t\t\t\t<th>Name</th><th>Result</th><th>Duration</th><th>Description</th>\n";
					echo "\t\t\t\t</tr>\n";

					$query5="SELECT * FROM results WHERE suite_id = " . $row4["id"];
					$result5=mysql_query($query5);
					while($row5 = mysql_fetch_array($result5)) {
						echo "\t\t\t\t<tr>\n";

						echo "\t\t\t\t\t<td>" . $row5["name"] . "</td><td bgcolor=\"";
						$testResult = $row5["result"];
						if ($testResult == "success") {
							echo "green";
							$totalPassed += 1;

						} else if ($testResult == "timeout") {
							echo "yellow";
							$totalFailed += 1;

						} else {
							echo "red";
							$totalFailed += 1;
						}
						echo "\">" . $row5["result"] . "</td><td>" . $row5["duration"] . "</td><td>" . $row5["description"] . "</td>\n";

						echo "\t\t\t\t</tr>\n";
					}
					echo "\t\t\t</table>\n";
					echo "\t\t</div>\n";
				}

				echo "\t</div>\n";
			}

			echo "</div>\n";
		}
	}

	echo "<!--  END GENERATED -->\n";
?>
		</div>

		<script type="text/javascript">
			updateSummary(<?php echo $totalPassed . ", " . $totalFailed; ?>);
		</script>
	</body>
</html>
