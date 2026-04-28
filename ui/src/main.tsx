import React from "react";
import { createRoot } from "react-dom/client";
import { TaskDashboardPage } from "./pages/TaskDashboardPage";
import "./styles.css";

createRoot(document.getElementById("root")!).render(<TaskDashboardPage />);
