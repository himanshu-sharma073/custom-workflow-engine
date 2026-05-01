import React from "react";
import { createRoot } from "react-dom/client";
import { TaskDashboardPage } from "./pages/TaskDashboardPage";
import "@fortawesome/fontawesome-free/css/fontawesome.min.css";
import "@fortawesome/fontawesome-free/css/regular.min.css";
import "./styles.css";

createRoot(document.getElementById("root")!).render(<TaskDashboardPage />);
